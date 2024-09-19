package ml.jk.android.budgetapp

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import java.util.*

class RaportsActivity : AppCompatActivity() {

    private lateinit var pieChart: PieChart // Wykres kołowy do wyświetlania danych
    private lateinit var monthSpinner: Spinner // Spinner do wyboru miesiąca
    private lateinit var totalExpensesText: TextView // Tekst do wyświetlania sumy wydatków
    private lateinit var totalRevenueText: TextView // Tekst do wyświetlania sumy przychodów
    private val db = FirebaseFirestore.getInstance() // Instancja Firestore

    // Listener do nasłuchiwania na zmiany w danych
    private var expensesListener: ListenerRegistration? = null
    private var revenueListener: ListenerRegistration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_raports) // Ustawienie widoku z layoutu

        // Inicjalizacja widoków
        pieChart = findViewById(R.id.reportsPieChart)
        monthSpinner = findViewById(R.id.monthSpinner)
        totalExpensesText = findViewById(R.id.totalExpensesText)
        totalRevenueText = findViewById(R.id.totalRevenueText)

        // Inicjalizacja spinnera z miesiącami
        val months = arrayOf(
            "January", "February", "March", "April", "May", "June",
            "July", "August", "September", "October", "November", "December"
        )
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, months)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        monthSpinner.adapter = adapter

        // Ustawienie akcji przycisku generowania raportu
        findViewById<Button>(R.id.generateReportButton).setOnClickListener {
            val selectedMonth = monthSpinner.selectedItemPosition // Pobranie wybranego miesiąca
            generateReportForMonth(selectedMonth) // Generowanie raportu dla wybranego miesiąca
        }

        // Rozpoczęcie nasłuchiwania na zmiany w wydatkach i przychodach
        startListeningToExpenses()
        startListeningToRevenue()
    }

    override fun onStop() {
        super.onStop()
        // Zatrzymanie nasłuchu, gdy aktywność nie jest widoczna
        expensesListener?.remove()
        revenueListener?.remove()
    }

    // Nasłuchiwanie na zmiany w wydatkach
    private fun startListeningToExpenses() {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_MONTH, 1) // Początek miesiąca
        val startOfMonth = calendar.timeInMillis // Początek miesiąca w milisekundach
        calendar.add(Calendar.MONTH, 1)
        val endOfMonth = calendar.timeInMillis // Koniec miesiąca w milisekundach

        // Nasłuchiwanie na zmiany w kolekcji "transactions" gdzie typ to "expense"
        expensesListener = db.collection("transactions")
            .whereEqualTo("type", "expense")
            .whereGreaterThanOrEqualTo("timestamp", startOfMonth)
            .whereLessThan("timestamp", endOfMonth)
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    totalExpensesText.text = "TOTAL EXPENSES: Error"
                    return@addSnapshotListener
                }

                var totalExpenses = 0.0
                snapshots?.let {
                    for (doc in it.documents) {
                        val amount = doc.getDouble("amount") ?: 0.0
                        totalExpenses += amount
                    }
                    totalExpensesText.text = "TOTAL EXPENSES: $totalExpenses PLN"
                } ?: run {
                    totalExpensesText.text = "TOTAL EXPENSES: 0 PLN"
                }
            }
    }

    // Nasłuchiwanie na zmiany w przychodach
    private fun startListeningToRevenue() {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_MONTH, 1) // Początek miesiąca
        val startOfMonth = calendar.timeInMillis // Początek miesiąca w milisekundach
        calendar.add(Calendar.MONTH, 1)
        val endOfMonth = calendar.timeInMillis // Koniec miesiąca w milisekundach

        // Nasłuchiwanie na zmiany w kolekcji "transactions" gdzie typ to "revenue"
        revenueListener = db.collection("transactions")
            .whereEqualTo("type", "revenue")
            .whereGreaterThanOrEqualTo("timestamp", startOfMonth)
            .whereLessThan("timestamp", endOfMonth)
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    totalRevenueText.text = "TOTAL REVENUE: Error"
                    return@addSnapshotListener
                }

                var totalRevenue = 0.0
                snapshots?.let {
                    for (doc in it.documents) {
                        val amount = doc.getDouble("amount") ?: 0.0
                        totalRevenue += amount
                    }
                    totalRevenueText.text = "TOTAL REVENUE: $totalRevenue PLN"
                } ?: run {
                    totalRevenueText.text = "TOTAL REVENUE: 0 PLN"
                }
            }
    }

    // Generowanie raportu dla wybranego miesiąca
    private fun generateReportForMonth(month: Int) {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.MONTH, month)
        calendar.set(Calendar.DAY_OF_MONTH, 1) // Początek miesiąca
        val startOfMonth = calendar.timeInMillis // Początek miesiąca w milisekundach

        calendar.add(Calendar.MONTH, 1)
        val endOfMonth = calendar.timeInMillis // Koniec miesiąca w milisekundach

        // Pobranie danych transakcji dla wybranego miesiąca
        db.collection("transactions")
            .whereGreaterThanOrEqualTo("timestamp", startOfMonth)
            .whereLessThan("timestamp", endOfMonth)
            .get()
            .addOnSuccessListener { result ->
                val categoryMap = mutableMapOf<String, Double>()

                for (document in result) {
                    val category = document.getString("category") ?: "Unknown"
                    val amount = document.getDouble("amount") ?: 0.0

                    // Sumowanie kwot w poszczególnych kategoriach
                    categoryMap[category] = categoryMap.getOrDefault(category, 0.0) + amount
                }

                // Wyświetlenie danych w wykresie kołowym
                displayPieChart(categoryMap)
            }
            .addOnFailureListener {
                // Wyświetlenie błędu, jeśli pobranie danych nie powiodło się
                Toast.makeText(this, "Failed to generate report", Toast.LENGTH_SHORT).show()
            }
    }

    // Wyświetlanie danych w wykresie kołowym
    private fun displayPieChart(data: Map<String, Double>) {
        val entries = mutableListOf<PieEntry>()

        // Konwersja danych na format akceptowany przez wykres kołowy
        data.forEach { (category, amount) ->
            entries.add(PieEntry(amount.toFloat(), category))
        }

        // Utworzenie zestawu danych i przypisanie go do wykresu
        val dataSet = PieDataSet(entries, "Categories")
        dataSet.valueTextSize = 12f  // Ustawienie rozmiaru tekstu etykiet
        val pieData = PieData(dataSet)

        pieChart.data = pieData
        pieChart.invalidate()  // Odświeżenie wykresu, aby zaktualizować widok
    }
}
