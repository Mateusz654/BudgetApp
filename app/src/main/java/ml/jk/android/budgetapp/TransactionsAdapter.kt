import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import ml.jk.android.budgetapp.R
import java.util.*

// Klasa danych reprezentująca pojedynczą transakcję
data class Transaction(
    val id: String, // Identyfikator dokumentu
    val amount: Double, // Kwota transakcji
    val category: String, // Kategoria transakcji
    val type: String, // Typ transakcji (np. przychód, wydatek)
    val description: String, // Opis transakcji
    val timestamp: Long // Czas transakcji w formie znacznika czasu (timestamp)
)

// Adapter do wyświetlania listy transakcji w RecyclerView
class TransactionsAdapter(
    private val transactionsList: List<Transaction>, // Lista transakcji do wyświetlenia
    private val deleteTransaction: (Transaction) -> Unit // Funkcja do usuwania transakcji
) : RecyclerView.Adapter<TransactionsAdapter.ViewHolder>() {

    // Klasa ViewHolder dla każdego elementu listy
    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val amountTextView: TextView = itemView.findViewById(R.id.transactionAmount) // TextView do wyświetlania kwoty
        val categoryTextView: TextView = itemView.findViewById(R.id.transactionCategory) // TextView do wyświetlania kategorii
        val descriptionTextView: TextView = itemView.findViewById(R.id.transactionDescription) // TextView do wyświetlania opisu
        val dateTextView: TextView = itemView.findViewById(R.id.transactionDate) // TextView do wyświetlania daty
        val deleteButton: ImageButton = itemView.findViewById(R.id.deleteTransactionButton) // Przycisk do usuwania transakcji
    }

    // Tworzenie nowego ViewHoldera dla danego elementu listy
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        // Inflacja widoku dla pojedynczego elementu listy z layoutu XML
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.transaction_item, parent, false)
        return ViewHolder(view) // Zwracanie nowego ViewHoldera
    }

    // Powiązanie danych z ViewHolderem
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val transaction = transactionsList[position] // Pobranie transakcji na danej pozycji

        // Ustawianie tekstu w TextView na podstawie danych transakcji
        holder.amountTextView.text = "Amount: ${transaction.amount}"
        holder.categoryTextView.text = "Category: ${transaction.category}"
        holder.descriptionTextView.text = "Description: ${transaction.description}"

        // Konwersja timestamp na czytelną datę w formacie "yyyy-MM-dd HH:mm"
        val date = DateFormat.format("yyyy-MM-dd HH:mm", Date(transaction.timestamp)).toString()
        holder.dateTextView.text = "Date: $date"

        // Obsługa kliknięcia przycisku usunięcia
        holder.deleteButton.setOnClickListener {
            deleteTransaction(transaction) // Wywołanie funkcji usuwania transakcji
        }
    }

    // Zwracanie liczby elementów w liście transakcji
    override fun getItemCount(): Int = transactionsList.size
}
