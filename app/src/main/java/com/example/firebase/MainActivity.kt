package com.example.firebase

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.lifecycle.MutableLiveData
import com.example.firebase.databinding.ActivityMainBinding
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : AppCompatActivity() {
    final lateinit var binding: ActivityMainBinding

    //INTI FIRESTORE
    private val firestore = FirebaseFirestore.getInstance()
    private val budgetCollectionRef = firestore.collection("budget")
    private var updateId = ""
    private val budgetListLiveData: MutableLiveData<List<Budget>> by lazy {
        MutableLiveData<List<Budget>>()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        with(binding){
            btnAdd.setOnClickListener {
                val nominal = edtNominal.text.toString()
                val desc = edtDesc.text.toString()
                val date = edtDate.text.toString()
                val newBudget = Budget(
                    nominal = nominal,
                    desc = desc,
                    date = date
                )
                addBudgets(newBudget)
            }

            btnUpdate.setOnClickListener {
                val nominal = edtNominal.text.toString()
                val desc = edtDesc.text.toString()
                val date = edtDate.text.toString()
                val updateData = Budget(
                    nominal = nominal,
                    desc = desc,
                    date = date
                )
                updateBudget(updateData)
                updateId = ""
                reset()
            }

            listView.setOnItemClickListener{ adapterView, view, position, id ->
                val item = adapterView.adapter.getItem(position) as Budget
                updateId = item.id
                edtNominal.setText(item.nominal)
                edtDesc.setText(item.desc)
                edtDate.setText(item.date)
            }

            listView.onItemLongClickListener = AdapterView.OnItemLongClickListener {
                    adapterView, view, position, id ->
                val item = adapterView.adapter.getItem(position) as Budget
                deleteBudget(item)
                true
            }

        }
        observeBudget()
        getAllBudgets()
    }

    private fun getAllBudgets(){
        observeBudgetChanges()
    }

    private fun observeBudgetChanges(){
        budgetCollectionRef.addSnapshotListener{ snapshots, error ->
            if (error != null){
                Log.d("MainActivity", "Error listening for budget changes: ", error)
                return@addSnapshotListener
            }
            val budgets = snapshots?.toObjects(Budget::class.java)
            if (budgets != null){
                budgetListLiveData.postValue(budgets)
            }
        }
    }

    private fun observeBudget(){
        budgetListLiveData.observe(this){
            budgets ->
            val adapter = ArrayAdapter(
                this,
                android.R.layout.simple_list_item_1,
                budgets.toMutableList()
            )
            binding.listView.adapter = adapter
        }
    }

    private fun addBudgets(budget: Budget){
        budgetCollectionRef.add(budget)
            .addOnSuccessListener {
                docRef ->
                val createBudgetId = docRef.id
                budget.id = createBudgetId
                docRef.set(budget)
                    .addOnFailureListener{
                        Log.d("MainActivity", "Failed Update id ", it)
                    }
                reset()
            }
            .addOnFailureListener{
                Log.d("MainActivity", "Failed to Add Budget", it)
            }
    }

    private fun reset(){
        with(binding){
            edtNominal.setText("")
            edtDesc.setText("")
            edtDate.setText("")
        }
    }

    private fun updateBudget(budget: Budget){
        budget.id = updateId
        budgetCollectionRef.document(updateId).set(budget)
            .addOnFailureListener{
                Log.d("MainActivity","Error Updating Budget: ", it)
            }
    }

    private fun deleteBudget(budget: Budget){
        if (budget.id.isEmpty()) {
            Log.d("MainActivity", "Error deleting: budget ID is empty!")
            return
        }
        budgetCollectionRef.document(budget.id).delete()
            .addOnFailureListener{
                Log.d("MainActivity", "Error deleting budget: ", it)
            }
    }
}