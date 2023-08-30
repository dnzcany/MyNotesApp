package com.denobaba.mynotes.view

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.Navigation
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.denobaba.mynotes.R
import com.denobaba.mynotes.adapter.NotesAdapter
import com.denobaba.mynotes.databinding.FragmentMainScreenBinding
import com.denobaba.mynotes.model.NotesModel
import com.denobaba.mynotes.viewmodel.NotesViewModel
import kotlinx.coroutines.launch

class MainScreen : Fragment() {
    private var _binding: FragmentMainScreenBinding? = null
    private val binding get() = _binding!!

    private val viewModel: NotesViewModel by viewModels()
    private lateinit var notesAdapter: NotesAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentMainScreenBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        notesAdapter = NotesAdapter(arrayListOf(),viewModel) // Önce NotesAdapter'ın bir örneğini oluşturun
        binding.recycleview.layoutManager = LinearLayoutManager(requireContext())
        binding.recycleview.adapter = notesAdapter


        binding.imageView6?.setOnClickListener {
            binding.searchBar.visibility = View.VISIBLE
            binding.imageView3.visibility = View.GONE
            binding.back1!!.visibility = View.VISIBLE
        }

        binding.back1?.setOnClickListener {
            binding.searchBar.visibility = View.GONE
            binding.imageView3.visibility = View.VISIBLE
            binding.back1!!.visibility= View.GONE
        }






        //sola kaydırma
        val itemTouchHelperCallback = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT ) {
            override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
                return false // We do not want to support moving items in this example
            }
            //posiition check
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val note = notesAdapter.getNoteAtPosition(position)
                showConfirmationDialog(note, position)
            }

            //pop up menu
            private fun showConfirmationDialog(note: NotesModel, position: Int) {
                val alertDialog = AlertDialog.Builder(requireContext())
                    .setTitle("Delete Note")
                    .setMessage("Are you sure you want to delete this note?")
                    .setPositiveButton("Yes") { _, _ ->
                        viewLifecycleOwner.lifecycleScope.launch {
                            viewModel.deleteNote(note) // Assuming you have this method in your ViewModel
                        }
                        //notesAdapter.removeAt(position)
                    }
                    .setNegativeButton("No") { dialog, _ ->
                        dialog.dismiss()
                        // Revert the swipe.
                        notesAdapter.notifyItemChanged(position)
                    }
                    .create()

                alertDialog.setOnShowListener {
                    alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.BLACK)
                    alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(Color.BLACK)
                }

                alertDialog.show()
            }

            //swipe and color change
            override fun onChildDraw(c: Canvas, recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, dX: Float, dY: Float, actionState: Int, isCurrentlyActive: Boolean) {
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)

                val itemView = viewHolder.itemView
                val paint = Paint()
                paint.color = Color.RED

                if (dX < 0) { // Swipe Left
                    c.drawRect(
                        itemView.right.toFloat() + dX,
                        itemView.top.toFloat(),
                        itemView.right.toFloat(),
                        itemView.bottom.toFloat(),
                        paint
                    )
                }

                val deleteIcon = ContextCompat.getDrawable(requireContext(), R.drawable.baseline_delete_24)
                deleteIcon?.let {
                    val iconMargin = (itemView.height - it.intrinsicHeight) / 2
                    val iconTop = itemView.top + (itemView.height - it.intrinsicHeight) / 2
                    val iconBottom = iconTop + it.intrinsicHeight
                    val iconRight = itemView.right - iconMargin
                    val iconLeft = itemView.right - iconMargin - it.intrinsicWidth

                    it.setBounds(iconLeft, iconTop, iconRight, iconBottom)
                    it.draw(c)
                }
            }

        }

        val itemTouchHelper = ItemTouchHelper(itemTouchHelperCallback)
        itemTouchHelper.attachToRecyclerView(binding.recycleview)

        //note update
        viewModel.allNotes.observe(viewLifecycleOwner) { notes ->
            notes?.let {
                notesAdapter.updatenoteslist(it)
                // Check if notes are empty or not
                if (it.isEmpty()) {
                    binding.animation.visibility = View.VISIBLE
                    binding.empty.visibility = View.VISIBLE
                } else {
                    binding.animation.visibility = View.GONE
                    binding.empty.visibility = View.GONE
                }
            }
        }

        //search yapma
        binding.searchBar.addTextChangedListener(object: TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // İsteğe bağlı olarak burada bir işlem yapabilirsiniz.
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // İsteğe bağlı olarak burada bir işlem yapabilirsiniz.
            }

            override fun afterTextChanged(s: Editable?) {
                val searchText = s.toString()
                notesAdapter.filter.filter(searchText)
            }
        })

        //fragment nav
        binding.buttonhere.setOnClickListener {
            val action = MainScreenDirections.actionMainScreenToNotes(-1)
            Navigation.findNavController(view).navigate(action)
        }




    }


}
