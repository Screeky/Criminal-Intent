package ru.mephi.criminalintent

import android.Manifest
import android.app.DatePickerDialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.text.Editable
import android.text.TextWatcher
import android.text.format.DateFormat
import android.util.Log
import android.view.*
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.core.content.FileProvider
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.exifinterface.media.ExifInterface
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*


private const val DATE_FORMAT = "EEE, MMM, dd"

class CrimeFragment : Fragment(), DatePickerDialog.OnDateSetListener, MenuProvider{

    private lateinit var crime: Crime
    private lateinit var titleField: EditText
    private lateinit var dateButton: Button
    private lateinit var solvedCheckBox: CheckBox
    private lateinit var reportButton: Button
    private lateinit var requiredPolice: CheckBox
    private lateinit var suspectButton: Button
    private lateinit var photoButton: ImageButton
    private lateinit var photoView: ImageView
    private lateinit var photoFile: File
    private lateinit var photoUri: Uri
    private val matrix = Matrix()


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
        if (suspect != "") {
            crime.suspect = suspect
            crimeDetailViewModel.saveCrime(crime)
            suspectButton.text = suspect
        }
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

    private val requestPermissionContacts = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            isGranted ->
        if (isGranted)
            getContact.launch()
        else {
            Toast.makeText(requireContext(), getString(R.string.permision_denied), Toast.LENGTH_SHORT).show()
        }
    }

    private val takePhoto = registerForActivityResult(ActivityResultContracts.TakePicture()){
        if (!it)
            Toast.makeText(requireContext(), getString(R.string.photo_not_taken), Toast.LENGTH_SHORT).show()
        else {
            var bmp = BitmapFactory.decodeFile(photoFile.path)
            bmp = rotatePhoto(bmp)
            val filesOutputStream = FileOutputStream(photoFile)
            bmp.compress(Bitmap.CompressFormat.JPEG, 85, filesOutputStream)
            filesOutputStream.flush()
            filesOutputStream.close()
            photoView.announceForAccessibility("Photo was taken")
        }
    }

    private val requestPermissionCamera = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
        isGranted ->
        if (isGranted)
            takePhoto.launch(photoUri)
        else {
            Toast.makeText(requireContext(), getString(R.string.permision_denied), Toast.LENGTH_SHORT).show()
        }
    }

    private val crimeDetailViewModel: CrimeDetailViewModel by lazy {
        ViewModelProvider(this@CrimeFragment, defaultViewModelProviderFactory)[CrimeDetailViewModel::class.java]
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
        val menuHost: MenuHost = requireActivity()
        menuHost.addMenuProvider(this, viewLifecycleOwner, Lifecycle.State.RESUMED)
        val view = inflater.inflate(R.layout.fragment_crime, container, false)
        titleField = view.findViewById(R.id.crime_title) as EditText
        dateButton = view.findViewById(R.id.crime_date) as Button
        solvedCheckBox = view.findViewById(R.id.crime_solved) as CheckBox
        reportButton = view.findViewById(R.id.crime_report) as Button
        requiredPolice = view.findViewById(R.id.required_police) as CheckBox
        suspectButton = view.findViewById(R.id.crime_suspect) as Button
        photoButton = view.findViewById(R.id.crime_camera) as ImageButton
        photoView = view.findViewById(R.id.crime_photo) as ImageView
        photoView.isEnabled = false

        if (CrimeFragmentArgs.fromBundle(requireArguments()).crimeId == null)
            reportButton.isEnabled = false
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        crimeDetailViewModel.crimeLiveData.observe(
            viewLifecycleOwner
        ) { crime ->
            crime?.let {
                this.crime = crime
                photoFile = crimeDetailViewModel.getPhotoFile(crime)
                Log.i("path", photoFile.toString())
                photoUri = FileProvider.getUriForFile(requireActivity(), "ru.mephi.criminalintent.fileprovider", photoFile)
                Log.i("uri", photoUri.toString())
                updateUI()
                BitmapFactory.decodeFile(photoFile.path)?.let {
                    photoView.setImageBitmap(Bitmap.createScaledBitmap(it, 800, 800, true))
                    photoView.isEnabled = true
                    photoView.contentDescription = getString(R.string.crime_photo_image_description)
                }
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
                requestPermissionContacts.launch(Manifest.permission.READ_CONTACTS)
        }

        photoButton.setOnClickListener {
            requestPermissionCamera.launch(Manifest.permission.CAMERA)
        }

        photoView.setOnClickListener {
                val action = CrimeFragmentDirections
                    .actionCrimeFragmentToPictureDialog(photoFile)
                this.findNavController().navigate(action)
            }
    }

    override fun onStop() {
        super.onStop()
        crimeDetailViewModel.saveCrime(crime)
    }

    private fun updateUI() {
        titleField.setText(crime.title)
        dateButton.text = SimpleDateFormat("EEE, MMM dd, yyyy HH:mm:ss z", Locale.getDefault()).format(this.crime.date)
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
        val suspect = if (crime.suspect.isBlank()) {
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

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menu.clear()
        menuInflater.inflate(R.menu.crime_fragment, menu)
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        if (menuItem.itemId == R.id.callSuspect) {
            if (crime.suspect != ""){
                Toast.makeText(requireContext(), getString(R.string.call_suspect), Toast.LENGTH_SHORT).show()
                val pattern = """\+?\d+""".toRegex(RegexOption.IGNORE_CASE)
                val number = pattern.find(crime.suspect)?.value?: ""
                val intent = Intent(Intent.ACTION_DIAL).apply { data = Uri.parse("tel:$number") }
                startActivity(intent)
            }
            else requestPermissionContacts.launch(Manifest.permission.READ_CONTACTS)
            return true
        }
        else return false
    }

    private fun rotatePhoto (bitmap: Bitmap): Bitmap{
        var rotatedBitmap = bitmap
        val exifInterface = ExifInterface(photoFile.path)
        val orientation = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED)
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> rotatedBitmap = rotateImage(bitmap, 90F)
            ExifInterface.ORIENTATION_ROTATE_180 -> rotatedBitmap = rotateImage(bitmap, 180F)
            ExifInterface.ORIENTATION_ROTATE_270 -> rotatedBitmap = rotateImage(bitmap, 270F)
        }
        return rotatedBitmap
    }

    private fun rotateImage(source: Bitmap, angle: Float): Bitmap {
        matrix.postRotate(angle)
        Log.i("rotate", matrix.toString())
        return Bitmap.createBitmap(
            source, 0, 0, source.width, source.height,
            matrix, true
        )
    }
}