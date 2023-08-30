package com.denobaba.mynotes.adapter

import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Filterable
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.navigation.Navigation
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.denobaba.mynotes.R
import com.denobaba.mynotes.databinding.RecycleRowBinding
import com.denobaba.mynotes.model.NotesModel
import com.denobaba.mynotes.view.MainScreenDirections
import com.denobaba.mynotes.viewmodel.NotesViewModel
import java.util.*
import java.util.logging.Filter
import kotlin.collections.ArrayList

class NotesAdapter(val noteslist: ArrayList<NotesModel>, val viewModel: NotesViewModel) :
    RecyclerView.Adapter<NotesAdapter.NotesHolder>(), Filterable {



    private var noteslistFiltered: ArrayList<NotesModel> = ArrayList()

    init {
        noteslistFiltered = noteslist
    }

    class NotesHolder(val binding: RecycleRowBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotesHolder {
        val binding = RecycleRowBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return NotesHolder(binding)
    }

    override fun getItemCount(): Int {
        return noteslistFiltered.size
    }

    @SuppressLint("MissingInflatedId")
    override fun onBindViewHolder(holder: NotesHolder, position: Int) {
        val note = noteslistFiltered[position]
        holder.binding.title.text = note.title
        holder.binding.date.text = note.date
        holder.binding.notes.text = note.notes

        if (note.locked){
            holder.binding.notes.setText(R.string.encrypted_text)
        }else{
            holder.binding.notes.text= note.notes
        }

        holder.binding.pin.visibility = if (note.pinned) View.VISIBLE else View.GONE


        holder.itemView.setOnLongClickListener {
            val noteToPin = noteslistFiltered[position]
            if(noteToPin.pinned) { // If note is already pinned, unpin it
                unpinNoteAtPosition(position)
            } else { // If note is not pinned, pin it
                pinNoteAtPosition(position)
            }
            true // Indicates that the long click was handled
        }





        note.uuid?.let { id ->
            holder.binding.lock.visibility = if (note.locked) View.VISIBLE else View.GONE
            holder.itemView.setOnClickListener {


                if (note.locked) { // Eğer not kilitliyse, şifreyi sor
                    holder.binding.lock.visibility = View.VISIBLE
                    val dialogView = LayoutInflater.from(holder.itemView.context).inflate(R.layout.dialog_password_control, null)
                    val passwordInput = dialogView.findViewById<EditText>(R.id.password11) // replace with your EditText id
                    val alertDialog = AlertDialog.Builder(holder.itemView.context)
                        .setView(dialogView)
                        .setPositiveButton("OK") { _, _ ->
                            if (passwordInput.text.toString() == note.password) { // Eğer girilen şifre doğruysa
                                val action = MainScreenDirections.actionMainScreenToNotes(id)
                                Navigation.findNavController(it).navigate(action)
                            } else { // Eğer şifre yanlışsa hata mesajı göster
                                Toast.makeText(holder.itemView.context, "Wrong password!", Toast.LENGTH_SHORT).show()
                            }
                        }
                        .setNegativeButton("Cancel", null)
                        .create()

                    alertDialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                    alertDialog.show()

                } else { // Eğer not kilitli değilse, direk notu aç
                    holder.binding.lock.visibility = View.GONE
                    val action = MainScreenDirections.actionMainScreenToNotes(id)
                    Navigation.findNavController(it).navigate(action)
                }
            }
        } ?: run {
            // handle null id here
        }

        holder.binding.alarm.visibility = if (note.alarmStatus) View.VISIBLE else View.GONE

    }


    fun getNoteAtPosition(position: Int): NotesModel {
        return noteslistFiltered[position]
    }


    fun unpinNoteAtPosition(position: Int) {
        val noteToUnpin = noteslistFiltered[position]

        // Remove the note from its current position and add it at the end
        noteslistFiltered.removeAt(position)
        noteslistFiltered.add(noteToUnpin)

        // Update the original list as well
        val originalNoteIndex = noteslist.indexOf(noteToUnpin)
        if (originalNoteIndex != -1) {
            noteslist.removeAt(originalNoteIndex)
            noteslist.add(noteToUnpin)
        }

        // Unset the pinned flag for the note
        noteToUnpin.pinned = false

        noteToUnpin.uuid?.let { viewModel.unpinNote(it) }


        notifyDataSetChanged()
    }

    fun updatenoteslist(newnoteslist: List<NotesModel>) {
        noteslist.clear()
        for (note in newnoteslist) {
            if (note.pinned) { // Eğer not pinlenmişse, listeye en başa ekleyin
                noteslist.add(0, note)
            } else { // Eğer not pinlenmemişse, listeye sona ekleyin
                noteslist.add(note)
            }
        }
        noteslistFiltered = ArrayList(noteslist) // filtered listeyi de güncelleyin
        notifyDataSetChanged()
    }



    override fun getFilter(): android.widget.Filter {
        return object : android.widget.Filter() {
            override fun performFiltering(charSequence: CharSequence): FilterResults {
                val charString = charSequence.toString()
                noteslistFiltered = if (charString.isEmpty()) {
                    noteslist
                } else {
                    val filteredList = ArrayList<NotesModel>()
                    for (note in noteslist) {
                        if (note.title?.toLowerCase(Locale.ROOT)!!.contains(charString.toLowerCase(Locale.ROOT))) {
                            filteredList.add(note)
                        }
                    }
                    filteredList
                }

                val filterResults = FilterResults()
                filterResults.values = noteslistFiltered
                return filterResults
            }

            override fun publishResults(charSequence: CharSequence, filterResults: FilterResults) {
                noteslistFiltered = filterResults.values as ArrayList<NotesModel>
                notifyDataSetChanged()
            }
        }
    }

    fun pinNoteAtPosition(position: Int) {
        val noteToPin = noteslistFiltered[position]

        // Remove the note from its current position and add it at the beginning
        noteslistFiltered.removeAt(position)
        noteslistFiltered.add(0, noteToPin)

        // Update the original list as well
        val originalNoteIndex = noteslist.indexOf(noteToPin)
        if (originalNoteIndex != -1) {
            noteslist.removeAt(originalNoteIndex)
            noteslist.add(0, noteToPin)
        }

        // Set the pinned flag for the note
        noteToPin.pinned = true

        noteToPin.uuid?.let { viewModel.pinNote(it) }


        notifyDataSetChanged()
    }







}
