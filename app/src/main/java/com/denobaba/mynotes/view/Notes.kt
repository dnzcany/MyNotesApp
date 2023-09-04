package com.denobaba.mynotes.view

import android.app.AlarmManager
import android.app.DatePickerDialog
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import android.widget.Toolbar
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProviders
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavOptions
import androidx.navigation.Navigation
import androidx.navigation.fragment.navArgs
import com.denobaba.mynotes.R
import com.denobaba.mynotes.adapter.NotesAdapter
import com.denobaba.mynotes.databinding.FragmentNotesBinding
import com.denobaba.mynotes.model.NotesModel
import com.denobaba.mynotes.util.NotificationReceiver
import com.denobaba.mynotes.viewmodel.NotesViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

class Notes : Fragment() {

    private var _binding: FragmentNotesBinding? = null
    private val binding get() = _binding!!
    private val viewModel: NotesViewModel by viewModels() // Use viewModels delegate instead of ViewModelProviders
    private val args: NotesArgs by navArgs() // Use navArgs to get the note UUID from arguments
    private var noteId: Int? = null
    private var isAlarmSet: Boolean = false
    private var notePinningListener: NotePinningListener? = null





    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentNotesBinding.inflate(layoutInflater,container,false)
        val view = binding.root
        return view
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setHasOptionsMenu(true)

        val toolbar: androidx.appcompat.widget.Toolbar = binding.toolbar
        (activity as AppCompatActivity).setSupportActionBar(toolbar)

        var originalTitle: String? = null
        var originalContent: String? = null



        // Get the note ID from arguments
        noteId = args.notesUuid

        lifecycleScope.launch {
            val alarmStatus = viewModel.getNoteAlarmStatus(noteId!!).await()
            // Alarm durumuna göre işlemlerinizi burada yapabilirsiniz
        }

        noteId?.let {
            val sharedPref = activity?.getPreferences(Context.MODE_PRIVATE) ?: return@let
            isAlarmSet = sharedPref.getBoolean("alarm_status_$it", false)
        }

        if (noteId != null && noteId != -1) {
            viewLifecycleOwner.lifecycleScope.launch {
                val note = viewModel.get(noteId!!)

                if (note != null) {
                    binding.titlehere.setText(note.title)
                    binding.noteshere.setText(note.notes)

                    originalTitle = note.title
                    originalContent = note.notes

                    // Set the isAlarmSet variable based on the note's alarmStatus
                    isAlarmSet = note.alarmStatus
                } else {
                    Log.e("Notes", "No note found with ID: $noteId")
                }
            }
        } else {
            // If no note is specified, clear the note fields
            binding.titlehere.setText("")
            binding.noteshere.setText("")
        }

        binding.backbutton.setOnClickListener {
            viewLifecycleOwner.lifecycleScope.launch {
                val isAlarmSet = noteId?.let { viewModel.get(it)?.alarmStatus } ?: false

                val title = binding.titlehere.text.toString()
                val noteText = binding.noteshere.text.toString()

                var noteChanged = false

                if (title != originalTitle || noteText != originalContent) {
                    noteChanged = true
                }

                if (title.isBlank() && noteText.isBlank()) {
                    Toast.makeText(context, "Notes empty", Toast.LENGTH_SHORT).show()
                } else {
                    val Enote = withContext(Dispatchers.IO) { viewModel.get(noteId!!) }

                    val formattedDate = if (noteChanged) {
                        val nowdate = LocalDateTime.now()
                        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                        nowdate.format(formatter)
                    } else {
                        Enote?.date
                    }

                    var password: String? = null
                    var locked = false
                    if (noteId != null && noteId != -1) {
                        password = viewModel.getNotePassword(noteId!!).await()
                        locked = viewModel.getNoteLockState(noteId!!).await()
                    }

                    val note = NotesModel(
                        title = title,
                        notes = noteText,
                        date = formattedDate,
                        uuid = if (noteId != null && noteId != -1) noteId else null,
                        password = password,
                        locked = locked,
                        alarmStatus = isAlarmSet
                    )

                    if (noteId != null && noteId != -1) {
                        withContext(Dispatchers.IO) { viewModel.updateNote(note) }
                        if (noteChanged) {
                            Toast.makeText(context, "Note saved", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        withContext(Dispatchers.IO) { viewModel.insertNote(note) }
                        Toast.makeText(context, "Note saved", Toast.LENGTH_SHORT).show()
                    }
                }

                val navOptions = NavOptions.Builder()
                    .setPopUpTo(R.id.notes, true)
                    .build()

                val action = NotesDirections.actionNotesToMainScreen()
                Navigation.findNavController(view).navigate(action, navOptions)
            }
        }




    }


    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        // Inflate the menu; this adds items to the action bar if it is present.
        inflater.inflate(R.menu.menu, menu) // Replace with your menu resource id
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            // Diğer case'ler buraya eklenecek

            R.id.pin -> {
                // Handle pin action
                //if user clicks on pin icon, then the note will be pinned.
                noteId?.let { notePinningListener?.onNotePinned(it) }
                return true

            }
            R.id.AddAlarm -> {
                val calendar = Calendar.getInstance()

                val timePickerDialog = TimePickerDialog(
                    context,
                    { _, hourOfDay, minute ->
                        calendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                        calendar.set(Calendar.MINUTE, minute)
                        calendar.set(Calendar.SECOND, 0)

                        val intent = Intent(context, NotificationReceiver::class.java)
                        intent.putExtra("noteId", noteId)
                        intent.putExtra("noteTitle", binding.titlehere.text.toString())

                        val pendingIntent = PendingIntent.getBroadcast(
                            context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT
                        )

                        val alarmManager = context?.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                        alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)

                        // Check if alarm is set
                        val alarmIsSet = PendingIntent.getBroadcast(
                            context, 0, intent, PendingIntent.FLAG_NO_CREATE) != null

                        if (alarmIsSet) {
                            viewModel.alarmStatus.value = true
                            noteId?.let { viewModel.updateAlarmStatus(it, true) }
                            Toast.makeText(context, "Alarm is set.", Toast.LENGTH_SHORT).show()
                        } else {
                            viewModel.alarmStatus.value = false
                            noteId?.let { viewModel.updateAlarmStatus(it, false) }
                            Toast.makeText(context, "Alarm is not set.", Toast.LENGTH_SHORT).show()
                        }

                        // Update the visibility of menu items
                        activity?.invalidateOptionsMenu()
                    },
                    calendar.get(Calendar.HOUR_OF_DAY),
                    calendar.get(Calendar.MINUTE),
                    true
                ).apply {
                    setOnShowListener {
                        getButton(DialogInterface.BUTTON_POSITIVE).setTextColor(Color.BLACK)
                        getButton(DialogInterface.BUTTON_NEGATIVE).setTextColor(Color.BLACK)
                    }
                }

                val datePickerDialog = DatePickerDialog(
                    requireContext(),
                    { _, year, month, dayOfMonth ->
                        calendar.set(year, month, dayOfMonth)
                        timePickerDialog.show()
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
                ).apply {
                    setOnShowListener {
                        getButton(DialogInterface.BUTTON_POSITIVE).setTextColor(Color.BLACK)
                        getButton(DialogInterface.BUTTON_NEGATIVE).setTextColor(Color.BLACK)
                    }
                }

                datePickerDialog.show()
                return true
            }

            R.id.DeleteAlarm -> {
                val intent = Intent(context, NotificationReceiver::class.java)
                val pendingIntent = PendingIntent.getBroadcast(
                    context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT
                )

                val alarmManager = context?.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                alarmManager.cancel(pendingIntent)
                viewModel.alarmStatus.value = false
                noteId?.let { viewModel.updateAlarmStatus(it, false) } // Save the alarm status in the database

                Toast.makeText(context, "Alarm cancelled.", Toast.LENGTH_SHORT).show()

                // Update the visibility of menu items
                activity?.invalidateOptionsMenu()
                return true
            }

            R.id.lock -> {
                val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_password, null)
                val password1 = dialogView.findViewById<EditText>(R.id.password1)
                val password2 = dialogView.findViewById<EditText>(R.id.password2)

                val alertDialog = AlertDialog.Builder(requireContext())
                    .setView(dialogView)
                    .setPositiveButton("Apply") { _, _ ->
                        // Apply password protection here
                        val password = password1.text.toString()
                        val passwordAgain = password2.text.toString()

                        if (password == passwordAgain) {
                            viewLifecycleOwner.lifecycleScope.launch {
                                noteId?.let { viewModel.encryptAndSaveNote(it, password) }
                                Toast.makeText(requireContext(), "Passwords are match", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            // If passwords don't match, show an error
                            Toast.makeText(requireContext(), "Passwords do not match", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .setNegativeButton("Cancel") { dialog, _ ->
                        dialog.dismiss()
                    }
                    .create()

                alertDialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                alertDialog.show()
                return true
            }

            R.id.unlock -> {
                val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_password_unlock, null)
                val passwordInput = dialogView.findViewById<EditText>(R.id.password1)

                val alertDialog = AlertDialog.Builder(requireContext())
                    .setView(dialogView)
                    .setPositiveButton("Apply") { _, _ ->
                        val password = passwordInput.text.toString()

                        viewLifecycleOwner.lifecycleScope.launch {
                            noteId?.let { id ->
                                val correctPassword = viewModel.getNotePassword(id).await()

                                if (password == correctPassword) {
                                    val note = viewModel.get(id)
                                    if (note != null) {
                                        val unlockedNote = note.copy(locked = false, password = null)
                                        viewModel.updateNote(unlockedNote)
                                        // Update locked status in ViewModel as well
                                        viewModel.updateLockedState(id, false)  // Assuming you have a method to update the locked state in ViewModel
                                        Toast.makeText(requireContext(), "Note unlocked", Toast.LENGTH_SHORT).show()

                                    }
                                } else {
                                    Toast.makeText(requireContext(), "Wrong password", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }
                    .setNegativeButton("Cancel") { dialog, _ ->
                        dialog.dismiss()
                    }
                    .create()

                // Set the dialog background to transparent
                alertDialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                alertDialog.show()
                return true
            }

            R.id.Share -> {
                // Get the title and content of the note
                val noteTitle = binding.titlehere.text.toString()
                val noteContent = binding.noteshere.text.toString()

                // Create a share intent
                val sendIntent: Intent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_SUBJECT, noteTitle) // Subject of the sharing content
                    putExtra(Intent.EXTRA_TEXT, noteContent) // Body of the sharing content
                    type = "text/plain"
                }

                // Create a chooser intent and start it
                val shareIntent = Intent.createChooser(sendIntent, null)
                startActivity(shareIntent)


                return true
            }

            else -> return super.onOptionsItemSelected(item)
        }
    }


    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)

        viewLifecycleOwner.lifecycleScope.launch {

            val alarmStatus = viewModel.getNoteAlarmStatus(noteId!!).await()

            val addAlarmItem = menu.findItem(R.id.AddAlarm)
            val deleteAlarmItem = menu.findItem(R.id.DeleteAlarm)

            if (alarmStatus == true) {
                addAlarmItem.isVisible = false
                deleteAlarmItem.isVisible = true
            } else {
                addAlarmItem.isVisible = true
                deleteAlarmItem.isVisible = false
            }
            noteId?.let { id ->
                val isLocked = viewModel.getNoteLockState(id).await()

                menu.findItem(R.id.lock).isVisible = !isLocked
                menu.findItem(R.id.unlock).isVisible = isLocked
            }





            try {
                if (menu.javaClass.simpleName == "MenuBuilder") {
                    val m = menu.javaClass.getDeclaredMethod(
                        "setOptionalIconsVisible",
                        java.lang.Boolean.TYPE
                    )
                    m.isAccessible = true
                    m.invoke(menu, true)
                }
            } catch (e: Exception) {
                Log.e(javaClass.simpleName, "onMenuOpened...unable to set icons for overflow menu", e)
            }
        }
    }


    interface NotePinningListener {
        fun onNotePinned(noteId: Int)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (parentFragment is NotePinningListener) {
            notePinningListener = parentFragment as NotePinningListener
        }
    }

    override fun onDetach() {
        super.onDetach()
        notePinningListener = null
    }



}