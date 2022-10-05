package ru.mephi.criminalintent

import android.Manifest
import android.app.DatePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.ContactsContract
import android.text.Editable
import android.text.TextWatcher
import android.text.format.DateFormat
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import java.util.*

private const val DATE_FORMAT = "EEE, MMM, dd"
private const val CONTACT_PERMISSION_CODE = 1

class CrimeFragment : Fragment(), DatePickerDialog.OnDateSetListener{

    private lateinit var crime: Crime
    private lateinit var titleField: EditText
    private lateinit var dateButton: Button
    private lateinit var solvedCheckBox: CheckBox
    private lateinit var reportButton: Button
    private lateinit var requiredPolice: CheckBox
    private lateinit var suspectButton: Button
    private lateinit var toolbar: Toolbar

    private val getContact = registerForActivityResult(ActivityResultContracts.PickContact()){ uri ->
        var suspect = ""
        val cursor1 = uri?.let { requireActivity().contentResolver.query(it,null,null,null,null)
        }
        if (cursor1 != null && cursor1.moveToFirst()){
            val contactId = cursor1.getString(cursor1.getColumnIndexOrThrow(ContactsContract.Contacts._ID))
            val contactName = cursor1.getString(cursor1.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME))
            val idResults = cursor1.getString(cursor1.getColumnIndexOrThrow(ContactsContract.Contacts.HAS_PHONE_NUMBER)).toInt()
            suspect = "$contactName "
            if (idResults == 1){
                val cursor2 = requireActivity().contentResolver.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    null,
                    ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = " + contactId,
                    null,
                null)
                if (cursor2 != null && cursor2.moveToFirst()){
                    val contactNumber = cursor2.getString(cursor2.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER))
                    suspect += contactNumber
                }
            }
        }
        crime.suspect = suspect
        crimeDetailViewModel.saveCrime(crime)
        suspectButton.text = suspect
        /*val cursor1 = uri?.let { it1 ->
            requireActivity().contentResolver
                .query(it1, queryFieldsName, null, null, null)
        }
        var suspect: String
        cursor1?.use {
            if (it.count == 0){
                return@use
            }
            it.moveToFirst()
            suspect = it.getString(0)
        }
        crime.suspect = suspect
        crimeDetailViewModel.saveCrime(crime)
        suspectButton.text = suspect*/
    }

    private var requestPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            isGranted ->
        if (isGranted)
            getContact.launch()
        else {
            Toast.makeText(requireContext(), "Permission denied", Toast.LENGTH_SHORT).show()
        }
    }


    private val crimeDetailViewModel: CrimeDetailViewModel by lazy {
        ViewModelProvider(this@CrimeFragment, defaultViewModelProviderFactory)[CrimeDetailViewModel::class.java]
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        crime = Crime()
        val crimeId: UUID? = CrimeFragmentArgs.fromBundle(requireArguments()).crimeId
        if (crimeId !== null)
            crimeDetailViewModel.loadCrime(crimeId)
        else {
            val newCrime = Crime()
            crimeDetailViewModel.addCrime(newCrime)
            crimeDetailViewModel.loadCrime(newCrime.id)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_crime, container, false)
        titleField = view.findViewById(R.id.crime_title) as EditText
        dateButton = view.findViewById(R.id.crime_date) as Button
        solvedCheckBox = view.findViewById(R.id.crime_solved) as CheckBox
        reportButton = view.findViewById(R.id.crime_report) as Button
        requiredPolice = view.findViewById(R.id.required_police) as CheckBox
        suspectButton = view.findViewById(R.id.crime_suspect) as Button
        toolbar = view.findViewById(R.id.toolbar) as Toolbar
        if (CrimeFragmentArgs.fromBundle(requireArguments()).crimeId == null)
            reportButton.isEnabled = false
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        toolbar.inflateMenu(R.menu.crime_fragment)
        toolbar.setOnMenuItemClickListener {
            Toast.makeText(requireContext(), "Calling the suspect", Toast.LENGTH_SHORT).show()
            return@setOnMenuItemClickListener true
        }
        crimeDetailViewModel.crimeLiveData.observe(
            viewLifecycleOwner
        ) { crime ->
            crime?.let {
                this.crime = crime
                updateUI()
            }
        }
    }

    override fun onStart() {
        super.onStart()

        val titleWatcher = object : TextWatcher {
            override fun beforeTextChanged(
                sequence: CharSequence?,
                start: Int,
                count: Int,
                after: Int
            ) { }
            override fun onTextChanged(
                sequence: CharSequence?,
                start: Int,
                before: Int,
                count: Int
            ) {
                crime.title = sequence.toString()
            }
            override fun afterTextChanged(sequence: Editable?) { }
        }

        titleField.addTextChangedListener(titleWatcher)

        solvedCheckBox.apply {
            setOnCheckedChangeListener { _, isChecked ->
                crime.isSolved = isChecked
            }
        }
        requiredPolice.apply {
            setOnCheckedChangeListener { _, isChecked ->
                crime.requiresPolice = isChecked
            }
        }

        dateButton.setOnClickListener {
            val oldCalendar = GregorianCalendar()
            oldCalendar.time = crime.date
            DatePickerDialog(
                requireContext(),
                this,
                oldCalendar.get(Calendar.YEAR),
                oldCalendar.get(Calendar.MONTH),
                oldCalendar.get(Calendar.DAY_OF_MONTH)
            ).show()
            /*val action = CrimeFragmentDirections.actionCrimeFragmentToDatePickerFragment(crime.date)
            val action1 = CrimeFragmentDirections.actionCrimeFragmentToDateAndTimePickerFragment(crime.date)
            /*val currentFragment = findNavController().getBackStackEntry(R.id.crimeFragment)
            val dialogObserver = LifecycleEventObserver{_, event ->
                if (event == Lifecycle.Event.ON_RESUME && currentFragment.savedStateHandle.contains("key")){
                    crime.date = currentFragment.savedStateHandle["key"]?: crime.date
                    updateUI()
                }
            }
            val dialogLifecycle = currentFragment.lifecycle
            dialogLifecycle.addObserver(dialogObserver)
            viewLifecycleOwner.lifecycle.addObserver(LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_DESTROY) {
                    dialogLifecycle.removeObserver(dialogObserver)
                }
            })*/
            findNavController().currentBackStackEntry?.savedStateHandle?.getLiveData<Date>("key")?.observe(viewLifecycleOwner
            ){resultDate -> resultDate?.let {
                crime.date = findNavController().getBackStackEntry(R.id.crimeFragment).savedStateHandle["key"]?: crime.date
                }
                updateUI()
            }
            findNavController().navigate(action)*/
        }

        reportButton.setOnClickListener {
            Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, getCrimeReport())
                putExtra(
                    Intent.EXTRA_SUBJECT,
                    getString(R.string.crime_report_subject))
            }.also { intent ->
                val chooserIntent = Intent.createChooser(intent, getString(R.string.send_report))
                startActivity(chooserIntent)
            }
        }

        suspectButton.setOnClickListener {
                requestPermission.launch(Manifest.permission.READ_CONTACTS)
        }
            /*if (checkContactPermission())
                getContact.launch()
            else {
                requestContactPermission()
                if (checkContactPermission())
                    getContact.launch()
                else Toast.makeText(requireContext(), "Permission denied", Toast.LENGTH_SHORT).show()
            }
        }*/
    }

    override fun onStop() {
        super.onStop()
        crimeDetailViewModel.saveCrime(crime)
        requireActivity().actionBar?.show()
    }

    private fun updateUI() {
        titleField.setText(crime.title)
        dateButton.text = crime.date.toString()
        solvedCheckBox.apply {
            isChecked = crime.isSolved
            jumpDrawablesToCurrentState()
        }
        requiredPolice.apply {
            isChecked = crime.requiresPolice
            jumpDrawablesToCurrentState()
        }
        if (crime.suspect != "")
            suspectButton.text = crime.suspect
    }

    override fun onDateSet(datePicker: DatePicker?, year: Int, month: Int, day: Int) {
        crime.date = GregorianCalendar(year, month, day).time
        updateUI()
        val action = CrimeFragmentDirections.actionCrimeFragmentToDatePickerFragment(crime.date)
        findNavController().currentBackStackEntry?.savedStateHandle?.getLiveData<Date>("key")?.observe(viewLifecycleOwner
        ){resultDate -> resultDate?.let {
            crime.date = findNavController().getBackStackEntry(R.id.crimeFragment).savedStateHandle["key"]?: crime.date
        }
            updateUI()
        }
        findNavController().navigate(action)
    }

    private fun getCrimeReport(): String {
        val solvedString = if (crime.isSolved) {
            getString(R.string.crime_report_solved)
        } else {
            getString(R.string.crime_report_unsolved)
        }
        val dateString = DateFormat.format(DATE_FORMAT, crime.date).toString()
        var suspect = if (crime.suspect.isBlank()) {
            getString(R.string.crime_report_no_suspect)
        } else {
            getString(R.string.crime_report_suspect, crime.suspect)
        }
        return getString(R.string.crime_report, crime.title, dateString, solvedString, suspect)
    }

    /*companion object {
        fun newInstance(crimeId: UUID): CrimeFragment {
            val args = Bundle().apply {
                putSerializable(ARG_CRIME_ID, crimeId)
            }
            return CrimeFragment().apply {
                arguments = args
            }
        }
    }*/
    private fun checkContactPermission(): Boolean{
            return ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.READ_CONTACTS
            ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestContactPermission() {
        val permission = arrayOf(Manifest.permission.READ_CONTACTS)
        ActivityCompat.requestPermissions(requireActivity(), permission, CONTACT_PERMISSION_CODE)
    }
}