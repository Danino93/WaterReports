package com.ashaf.instanz.utils

/**
 * Utility class for calculating quote totals, VAT, and subtotals
 */
object QuoteCalculator {
    
    /**
     * Calculate total price from unit price and quantity
     */
    fun calculateTotal(unitPrice: Double, quantity: Double): Double {
        return unitPrice * quantity
    }
    
    /**
     * Calculate VAT amount from subtotal and VAT rate
     */
    fun calculateVat(subtotal: Double, vatRate: Double): Double {
        return subtotal * (vatRate / 100.0)
    }
    
    /**
     * Calculate total including VAT
     */
    fun calculateTotalWithVat(subtotal: Double, vatRate: Double): Double {
        val vatAmount = calculateVat(subtotal, vatRate)
        return subtotal + vatAmount
    }
    
    /**
     * Calculate subtotal from work sections
     */
    fun calculateSubtotal(workItems: List<WorkItem>): Double {
        return workItems.sumOf { it.totalPrice }
    }
    
    /**
     * Format price as Israeli Shekel string
     */
    fun formatPrice(amount: Double): String {
        return String.format("%.2f ש\"ח", amount)
    }
    
    /**
     * Parse price string to double
     */
    fun parsePrice(priceString: String): Double {
        return try {
            priceString.replace(",", "")
                .replace("ש\"ח", "")
                .replace(" ", "")
                .toDoubleOrNull() ?: 0.0
        } catch (e: Exception) {
            0.0
        }
    }
    
    data class WorkItem(
        val quantity: Double = 1.0,
        val unitPrice: Double = 0.0,
        val totalPrice: Double = 0.0
    )
    
    data class QuoteSummary(
        val subtotal: Double,
        val vatRate: Double,
        val vatAmount: Double,
        val totalWithVat: Double
    )
    
    /**
     * Calculate complete quote summary
     */
    fun calculateQuoteSummary(workItems: List<WorkItem>, vatRate: Double): QuoteSummary {
        val subtotal = calculateSubtotal(workItems)
        val vatAmount = calculateVat(subtotal, vatRate)
        val totalWithVat = subtotal + vatAmount
        
        return QuoteSummary(
            subtotal = subtotal,
            vatRate = vatRate,
            vatAmount = vatAmount,
            totalWithVat = totalWithVat
        )
    }
}

