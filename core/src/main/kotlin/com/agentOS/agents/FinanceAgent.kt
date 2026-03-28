package com.agentOS.agents

import com.agentOS.ai.GeminiClient
import com.agentOS.api.Agent
import com.agentOS.api.AgentAPI
import com.agentOS.api.AgentScope
import com.agentOS.api.ChatMessage
import com.agentOS.api.StorageAPI
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID
import kotlin.math.abs

data class Transaction(
    val id: String,
    val description: String,
    val amount: Double,     // positive = income, negative = expense
    val category: String,
    val timestamp: Long
)

class FinanceAgent(private val storage: StorageAPI, private val gemini: GeminiClient) : Agent() {

    override val scope = AgentScope(
        id = "com.agentOS.finance",
        name = "Finance Agent",
        version = "0.1.0",
        author = "Cameron",
        description = "Conversational budgeting, spending tracking, and financial summaries.",
        capabilities = setOf("storage", "ui")
    )

    override val api: AgentAPI = NoOpAgentAPI(storage)

    private val dateFmt = DateTimeFormatter.ofPattern("MMM d, yyyy")
        .withZone(ZoneId.systemDefault())

    override suspend fun onChat(message: ChatMessage): ChatMessage {
        val text = message.text.trim()
        val lower = text.lowercase()

        val response = when {
            lower.startsWith("add transaction:") || lower.startsWith("add expense:") || lower.startsWith("add income:") -> {
                val rest = text.substringAfter(":").trimStart()
                val isIncome = lower.startsWith("add income:")
                addTransaction(rest, isIncome)
            }
            lower.startsWith("add transaction ") -> addTransaction(text.substring(16).trim(), false)
            lower == "list transactions" || lower == "transactions" -> listTransactions(null)
            lower.startsWith("list transactions ") || lower.startsWith("transactions ") -> {
                val filter = text.substringAfterLast(" ").trim()
                listTransactions(filter)
            }
            lower == "budget summary" || lower == "summary" || lower == "balance" -> budgetSummary()
            lower.startsWith("set budget:") || lower.startsWith("set budget ") -> {
                val rest = text.substringAfter(if (lower.startsWith("set budget:")) ":" else " ").trimStart()
                setBudget(rest)
            }
            lower == "spending this month" || lower == "this month" -> spendingThisMonth()
            lower == "income this month" -> incomeThisMonth()
            lower.startsWith("delete transaction ") -> deleteTransaction(text.substring(19).trim())
            lower.startsWith("spending by category") || lower == "by category" -> spendingByCategory()
            else -> aiChat(text)
        }
        return ChatMessage("assistant", response)
    }

    private suspend fun addTransaction(input: String, forceIncome: Boolean): String {
        // Format: <description> | <amount> | [category]
        val parts = input.split("|", limit = 3)
        if (parts.isEmpty()) return "Format: add transaction: <description> | <amount> | [category]"

        val description = parts[0].trim()
        val amountStr = parts.getOrElse(1) { "" }.trim()
        val category = parts.getOrElse(2) { "Uncategorized" }.trim().ifBlank { "Uncategorized" }

        if (description.isBlank()) return "Description cannot be empty."

        val rawAmount = amountStr.replace("$", "").replace(",", "").toDoubleOrNull()
            ?: return "Invalid amount: \"$amountStr\". Use a number like 25.99 or -25.99."
        if (rawAmount.isNaN() || rawAmount.isInfinite()) return "Invalid amount: \"$amountStr\"."

        // Positive = income, negative = expense. forceIncome overrides sign
        val amount = when {
            forceIncome -> abs(rawAmount)
            rawAmount > 0 -> -rawAmount  // default unsigned positive values to expense
            else -> rawAmount
        }

        val id = UUID.randomUUID().toString().take(8)
        val txn = Transaction(
            id = id,
            description = description,
            amount = amount,
            category = category,
            timestamp = System.currentTimeMillis()
        )
        storage.writeString("txn_$id", serializeTransaction(txn))

        val sign = if (amount >= 0) "+" else ""
        return "Transaction recorded [ID: $id] $description: ${sign}${formatAmount(amount)} ($category)"
    }

    private suspend fun listTransactions(filter: String?): String {
        var txns = getAllTransactions().sortedByDescending { it.timestamp }
        if (filter != null && filter.isNotBlank()) {
            txns = txns.filter { t ->
                t.category.contains(filter, ignoreCase = true) ||
                    t.description.contains(filter, ignoreCase = true)
            }
        }
        if (txns.isEmpty()) {
            return if (filter != null) "No transactions for \"$filter\"." else "No transactions recorded."
        }
        return buildString {
            appendLine("Transactions${if (filter != null) " ($filter)" else ""} (${txns.size}):")
            for (t in txns) {
                val date = dateFmt.format(Instant.ofEpochMilli(t.timestamp))
                val sign = if (t.amount >= 0) "+" else ""
                appendLine("  [${t.id}] $date | ${t.description} | ${sign}${formatAmount(t.amount)} | ${t.category}")
            }
        }.trimEnd()
    }

    private suspend fun budgetSummary(): String {
        val txns = getAllTransactions()
        val totalIncome = txns.filter { it.amount > 0 }.sumOf { it.amount }
        val totalExpenses = txns.filter { it.amount < 0 }.sumOf { it.amount }
        val balance = totalIncome + totalExpenses

        // Load budgets
        val budgets = getBudgets()

        return buildString {
            appendLine("=== Budget Summary ===")
            appendLine("Total Income:   +${formatAmount(totalIncome)}")
            appendLine("Total Expenses: ${formatAmount(totalExpenses)}")
            appendLine("Balance:        ${if (balance >= 0) "+" else ""}${formatAmount(balance)}")
            if (budgets.isNotEmpty()) {
                appendLine()
                appendLine("=== Budget Limits ===")
                for ((cat, limit) in budgets) {
                    val spent = txns.filter { it.category.equals(cat, ignoreCase = true) && it.amount < 0 }
                        .sumOf { abs(it.amount) }
                    val pct = if (limit > 0) (spent / limit * 100).toInt() else 0
                    val bar = "█".repeat(pct / 10) + "░".repeat(10 - pct / 10)
                    appendLine("  $cat: ${formatAmount(spent)} / ${formatAmount(limit)} [$bar] $pct%")
                }
            }
        }.trimEnd()
    }

    private suspend fun setBudget(input: String): String {
        // Format: <category> | <amount>
        val parts = input.split("|", limit = 2)
        if (parts.size < 2) return "Format: set budget: <category> | <amount>"
        val category = parts[0].trim()
        val amountStr = parts[1].trim().replace("$", "").replace(",", "")
        val amount = amountStr.toDoubleOrNull() ?: return "Invalid amount: $amountStr"
        if (amount <= 0) return "Budget amount must be positive."

        storage.writeString("budget_${category.lowercase()}", "$category|$amount")
        return "Budget set: $category — ${formatAmount(amount)}/month"
    }

    private suspend fun spendingThisMonth(): String {
        val startOfMonth = LocalDate.now().withDayOfMonth(1)
            .atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

        val txns = getAllTransactions().filter { it.timestamp >= startOfMonth && it.amount < 0 }
            .sortedByDescending { it.timestamp }

        if (txns.isEmpty()) return "No expenses recorded this month."

        val total = txns.sumOf { it.amount }
        return buildString {
            appendLine("Spending this month (${txns.size} transactions):")
            for (t in txns) {
                val date = dateFmt.format(Instant.ofEpochMilli(t.timestamp))
                appendLine("  $date | ${t.description} | ${formatAmount(t.amount)} | ${t.category}")
            }
            appendLine("Total: ${formatAmount(total)}")
        }.trimEnd()
    }

    private suspend fun incomeThisMonth(): String {
        val startOfMonth = LocalDate.now().withDayOfMonth(1)
            .atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

        val txns = getAllTransactions().filter { it.timestamp >= startOfMonth && it.amount > 0 }
            .sortedByDescending { it.timestamp }

        if (txns.isEmpty()) return "No income recorded this month."
        val total = txns.sumOf { it.amount }
        return buildString {
            appendLine("Income this month:")
            for (t in txns) {
                val date = dateFmt.format(Instant.ofEpochMilli(t.timestamp))
                appendLine("  $date | ${t.description} | +${formatAmount(t.amount)} | ${t.category}")
            }
            appendLine("Total: +${formatAmount(total)}")
        }.trimEnd()
    }

    private suspend fun spendingByCategory(): String {
        val txns = getAllTransactions().filter { it.amount < 0 }
        if (txns.isEmpty()) return "No expenses recorded."

        val byCategory = txns.groupBy { it.category }
            .mapValues { (_, v) -> v.sumOf { abs(it.amount) } }
            .entries.sortedByDescending { it.value }

        val total = byCategory.sumOf { it.value }
        return buildString {
            appendLine("Spending by category:")
            for ((cat, amount) in byCategory) {
                val pct = if (total > 0) (amount / total * 100).toInt() else 0
                appendLine("  $cat: ${formatAmount(amount)} ($pct%)")
            }
            appendLine("Total: ${formatAmount(total)}")
        }.trimEnd()
    }

    private suspend fun deleteTransaction(id: String): String {
        val existing = storage.readString("txn_$id") ?: return "Transaction not found: $id"
        storage.delete("txn_$id")
        val txn = deserializeTransaction(existing)
        return "Deleted transaction [${id}]${if (txn != null) ": ${txn.description}" else ""}"
    }

    private suspend fun getAllTransactions(): List<Transaction> {
        return storage.listKeys()
            .filter { it.startsWith("txn_") }
            .mapNotNull { key -> storage.readString(key)?.let { deserializeTransaction(it) } }
    }

    private suspend fun getBudgets(): Map<String, Double> {
        return storage.listKeys()
            .filter { it.startsWith("budget_") }
            .mapNotNull { key ->
                val data = storage.readString(key) ?: return@mapNotNull null
                val parts = data.split("|", limit = 2)
                if (parts.size < 2) return@mapNotNull null
                parts[0] to (parts[1].toDoubleOrNull() ?: return@mapNotNull null)
            }.toMap()
    }

    private fun formatAmount(amount: Double): String {
        return "$%.2f".format(abs(amount)).let { if (amount < 0) "-$it" else it }
    }

    private fun serializeTransaction(t: Transaction): String {
        val desc = t.description.replace("|", "\\|")
        return "${t.id}|$desc|${t.amount}|${t.category}|${t.timestamp}"
    }

    private fun deserializeTransaction(data: String): Transaction? {
        val parts = data.split("|", limit = 5)
        if (parts.size < 5) return null
        return try {
            Transaction(
                id = parts[0],
                description = parts[1].replace("\\|", "|"),
                amount = parts[2].toDouble(),
                category = parts[3],
                timestamp = parts[4].toLong()
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun aiChat(userMessage: String): String {
        return gemini.chat(
            systemPrompt = """You are the Finance Agent for AgentOS. You help users track spending, income, and budgets.
Available commands: add transaction: <description> | <amount> | [category], add expense: <description> | <amount> | [category], add income: <description> | <amount> | [category], list transactions, budget summary, set budget: <category> | <amount>, spending this month, income this month, spending by category, delete transaction <id>.
Amounts: positive for income, negative for expenses (default is expense).
Be helpful and concise.""",
            userMessage = userMessage
        )
    }
}
