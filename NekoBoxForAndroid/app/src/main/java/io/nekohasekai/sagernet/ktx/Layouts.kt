package io.nekohasekai.sagernet.ktx

import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class FixedLinearLayoutManager(val recyclerView: RecyclerView) :
    LinearLayoutManager(recyclerView.context, RecyclerView.VERTICAL, false) {

    override fun onLayoutChildren(recycler: RecyclerView.Recycler?, state: RecyclerView.State?) {
        try {
            super.onLayoutChildren(recycler, state)
        } catch (ignored: IndexOutOfBoundsException) {
        }
    }

}

/** 同 FixedLinearLayoutManager，但用 columns 控制列数（1 = 单列，2 = 双列）。 */
class FixedGridLayoutManager(
    val recyclerView: RecyclerView,
    initialColumns: Int = 1,
) : GridLayoutManager(recyclerView.context, initialColumns, RecyclerView.VERTICAL, false) {

    /** 映射到 spanCount；变化时 GridLayoutManager 会自动重新布局 */
    var columns: Int
        get() = spanCount
        set(value) {
            if (spanCount != value) spanCount = value
        }

    override fun onLayoutChildren(recycler: RecyclerView.Recycler?, state: RecyclerView.State?) {
        try {
            super.onLayoutChildren(recycler, state)
        } catch (ignored: IndexOutOfBoundsException) {
        }
    }

}
