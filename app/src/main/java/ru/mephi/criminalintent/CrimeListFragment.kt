package ru.mephi.criminalintent

import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.findNavController
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*

private const val TAG = "CrimeListFragment"

class CrimeListFragment: Fragment() {

    private lateinit var crimeRecyclerView: RecyclerView
    private var adapter: CrimeListAdapter = CrimeListAdapter()
    private lateinit var addButton: Button
    private lateinit var emptyText: TextView

    private val crimeListViewModel: CrimeListViewModel by lazy {
        //val factory = defaultViewModelProviderFactory
        val factory = CrimeListViewModelFactory()
        ViewModelProvider(this@CrimeListFragment, factory)[CrimeListViewModel::class.java]
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Log.i(TAG, "on Create View called")
        val view = inflater.inflate(R.layout.fragment_crime_list, container, false)
        crimeRecyclerView = view.findViewById(R.id.crime_recycler_view) as RecyclerView
        crimeRecyclerView.layoutManager = LinearLayoutManager(context)
        addButton = view.findViewById(R.id.add_button) as Button
        emptyText = view.findViewById(R.id.empty_list) as TextView
        crimeRecyclerView.adapter = adapter

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.i(TAG, "on View Created called")
        crimeListViewModel.crimeListLiveData.observe(
            viewLifecycleOwner
        ) { crimes ->
            crimes?.let {
                Log.i(TAG, "Got crimes ${crimes.size}")
                updateUI(crimes)
                if (crimes.isNotEmpty()) {
                    addButton.visibility = View.GONE
                    emptyText.visibility = View.GONE
                }
            }
        }
    }

    private fun updateUI(crimes: List<Crime>){
        adapter.submitList(crimes)
        if (adapter.itemCount == 0){
            addButton.visibility = View.VISIBLE
            emptyText.visibility = View.VISIBLE
            addButton.setOnClickListener{
                val action = CrimeListFragmentDirections
                    .actionCrimeListFragmentToCrimeFragment()
                it.findNavController().navigate(action)
            }
        }
    }

    private abstract inner class CrimeHolder(view: View)
        : RecyclerView.ViewHolder(view), View.OnClickListener {

        lateinit var crime: Crime

        private val titleTextView: TextView = itemView.findViewById(R.id.crime_title)
        private val dateTextView: TextView = itemView.findViewById(R.id.crime_date)
        private val solvedImageView: ImageView = itemView.findViewById(R.id.crime_solved)

        open fun bind (crime: Crime) {
            this.crime = crime
            titleTextView.text = this.crime.title
            dateTextView.text = SimpleDateFormat("EEEE, MMM dd, yyyy", Locale.ENGLISH).format(this.crime.date)
            solvedImageView.visibility = if (crime.isSolved){
                View.VISIBLE
            } else {
                View.GONE
            }
        }
        override fun onClick(v: View?) {
            val action = CrimeListFragmentDirections
                .actionCrimeListFragmentToCrimeFragment()
            itemView.findNavController().navigate(action.setCrimeId(crime.id))
        }
    }

    private inner class NormalCrimeHolder(view: View) : CrimeHolder(view) {
        init {
            itemView.setOnClickListener(this)
        }
    }

    private inner class SeriousCrimeHolder(view: View) : CrimeHolder(view) {
        private val contactPoliceButton: Button = itemView.findViewById(R.id.button)
        init {
            itemView.setOnClickListener(this)
        }

        override fun bind(crime: Crime){
            super.bind(crime)
            contactPoliceButton.setOnClickListener{Toast
                .makeText(context, "Calling Police", Toast. LENGTH_SHORT)
                .show()
            }
        }
    }

    private inner class CrimeListAdapter: ListAdapter<Crime, CrimeHolder>(DiffCallback){

        override fun getItemViewType(position: Int): Int {
            return if (currentList[position].requiresPolice)
                1
            else 0
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CrimeHolder {
            return if (viewType == 0) {
                val view: View = layoutInflater.inflate(R.layout.list_item_crime, parent, false)
                NormalCrimeHolder(view)
            } else {
                val view: View = layoutInflater.inflate(R.layout.list_item_crime_required_police, parent, false)
                SeriousCrimeHolder(view)
            }
        }

        override fun onBindViewHolder(holder: CrimeHolder, position: Int) {
            val crime = currentList[position]
            holder.bind(crime)
        }

        override fun getItemCount() = currentList.size
    }

    object DiffCallback: DiffUtil.ItemCallback<Crime>(){
        override fun areItemsTheSame(oldItem: Crime, newItem: Crime): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Crime, newItem: Crime): Boolean {
            return oldItem == newItem
        }
    }

    override fun onStart() {
        super.onStart()
        Log.i(TAG, "on Start called")
    }
    override fun onResume() {
        super.onResume()
        Log.i(TAG, "on Resume called")
    }
    override fun onPause() {
        super.onPause()
        Log.i(TAG, "on Pause called")
    }

    override fun onStop() {
        super.onStop()
        Log.i(TAG, "on Stop called")
    }

    override fun onDestroyView() {
        super.onDestroyView()
    }
}