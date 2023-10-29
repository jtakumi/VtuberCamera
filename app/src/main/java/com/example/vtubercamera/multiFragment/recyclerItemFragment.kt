package com.example.vtubercamera.multiFragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.vtubercamera.R
import com.example.vtubercamera.databinding.FragmentRecyclerItemBinding
import com.example.vtubercamera.multiFragment.placeholder.PlaceholderContent

/**
 * A fragment representing a list of Items.
 */
class recyclerItemFragment : Fragment(), BackButtonCallBack {

    private var columnCount = 1
    private lateinit var binding: FragmentRecyclerItemBinding

    companion object {

        // TODO: Customize parameter argument names
        const val ARG_COLUMN_COUNT = "column-count"

        // TODO: Customize parameter initialization
        @JvmStatic
        fun newInstance(columnCount: Int) =
            recyclerItemFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_COLUMN_COUNT, columnCount)
                }
            }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = FragmentRecyclerItemBinding.inflate(layoutInflater)
        arguments?.let {
            columnCount = it.getInt(ARG_COLUMN_COUNT)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_recycler_item_list, container, false)

        // Set the adapter
        if (view is RecyclerView) {
            with(view) {
                layoutManager = when {
                    columnCount <= 1 -> LinearLayoutManager(context)
                    else -> GridLayoutManager(context, columnCount)
                }
                adapter = MyItemRecyclerViewAdapter(PlaceholderContent.ITEMS)
            }
        }
        return view
    }
    

    fun pullToCount() {
        val pullcount = 0
    }

    override fun onBackButtonPressed() {
        refreshAuto()
    }

    private fun refreshAuto() {
        binding.recyclerItemRefresh.text = getString(R.string.isRefresh)
    }
}