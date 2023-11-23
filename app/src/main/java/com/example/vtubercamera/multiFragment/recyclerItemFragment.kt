package com.example.vtubercamera.multiFragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
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
    private val presenter = MultiFragmentPresenter()

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
        NavigationTracker.setPreviousActivity(this.javaClass)
        val parentFragmentManager = parentFragmentManager
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_recycler_item_list, container, false)
        val recyclerView = view.findViewById<RecyclerView>(R.id.recycler_list)
        val linerLayoutManager = LinearLayoutManager(view.context)
        // Set the adapter
        if (view is RecyclerView) {
            with(view) {
                layoutManager = when {
                    columnCount <= 1 -> LinearLayoutManager(context)
                    else -> GridLayoutManager(context, columnCount)
                }
                val adapter = MyItemRecyclerViewAdapter(PlaceholderContent.ITEMS)
                recyclerView.layoutManager = linerLayoutManager
                recyclerView.adapter = adapter
                recyclerView.addItemDecoration(
                    DividerItemDecoration(
                        view.context,
                        linerLayoutManager.orientation
                    )
                )
                adapter.setOnItemClickListener(
                    object : MyItemRecyclerViewAdapter.onClickRecyclerItemListener {
                        override fun onItemClick(item: PlaceholderContent.PlaceholderItem) {
                            parentFragmentManager.beginTransaction()
                                .replace(R.id.multi_fragment_container, secondFragment())
                                .addToBackStack(null).commit()
                        }
                    }
                )
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