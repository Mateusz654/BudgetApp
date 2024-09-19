package ml.jk.android.budgetapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

// Adapter do wyświetlania listy kategorii w RecyclerView
class CategoriesAdapter(
    private val categoriesList: List<String>, // Lista kategorii do wyświetlenia
    private val deleteCategory: (String) -> Unit // Funkcja wywoływana przy próbie usunięcia kategorii
) : RecyclerView.Adapter<CategoriesAdapter.ViewHolder>() {

    // Klasa ViewHolder przechowująca widoki pojedynczego elementu listy
    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val categoryName: TextView = itemView.findViewById(R.id.categoryName) // Widok dla nazwy kategorii
        val deleteButton: Button = itemView.findViewById(R.id.deleteButton) // Widok dla przycisku usuwania
    }

    // Tworzenie nowego ViewHoldera
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        // Inflacja layoutu pojedynczego elementu listy (kategorii)
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.category_item, parent, false)
        return ViewHolder(view) // Zwrócenie nowego ViewHoldera
    }

    // Przypisanie danych do widoków w ViewHolderze
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val category = categoriesList[position] // Pobranie kategorii na podstawie pozycji
        holder.categoryName.text = category // Ustawienie nazwy kategorii w widoku
        holder.deleteButton.setOnClickListener {
            deleteCategory(category) // Wywołanie funkcji usuwania kategorii po kliknięciu przycisku
        }
    }

    // Zwrócenie liczby elementów w liście
    override fun getItemCount(): Int = categoriesList.size
}
