package org.techtown.diary


import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.fragment_1.view.*



class Fragment1 : Fragment() {

    private var _context: Context? = null
    lateinit var recyclerView : RecyclerView
    lateinit var adapter: NoteAdapter
    private var listener: OnTabItemSelectedListener? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        this._context = context

        if(context is OnTabItemSelectedListener){
            listener = context
        } // is == 자바의 instanceof (자료형의 일치)
    } // 프래그먼트가 액티비티에 올라갈 때 호출

    override fun onDetach() {
        super.onDetach()

        if (_context != null){
            _context = null
            listener = null
        }
    } // 액티비티에서 내려갈 때

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        var rootView = inflater.inflate(R.layout.fragment_1, container, false) as ViewGroup
        // as 형변환

        initUI(rootView)

        return rootView
    }

    private fun initUI (rootView : ViewGroup){
        rootView.todayWriteButton.setOnClickListener{
            listener?.onTabSelected(1)
        } // 클릭시 작성 레이아웃으로

        rootView.switchButton.setOnSwitchListener { position, tabText ->
            adapter.switchLayout(position)
            adapter.notifyDataSetChanged()
        } // 내용,사진 위주의 레이아웃으로 바꿔주고 갱신

        adapter = NoteAdapter(arrayListOf<Note>())

        recyclerView = rootView.recyclerView
        val layoutManager = LinearLayoutManager(context)
        recyclerView.layoutManager = layoutManager


        adapter.addItem(
            Note(
                0,
                "0",
                "전주시 덕진구",
                "123",
                "",
                "1. 테스트중!",
                "0",
                "capture1.jpg",
                "3월 14일"
            )
        )
        adapter.addItem(
            Note(
                1,
                "1",
                "군산시 나운동",
                "",
                "",
                "2. 안녕하세요",
                "1",
                "capture1.jpg",
                "1월 1일"
            )
        )
        adapter.addItem(
            Note(
                2,
                "2",
                "전북대학교",
                "",
                "",
                "3. ABC123",
                "2",
                null,
                "8월 5일"
            )
        ) // 테스트용 임의 아이템 3개
        recyclerView.adapter = adapter
        // 리싸이클러뷰에 어댑터 연결

        adapter.setOnItemClickListener(object : OnNoteItemClickListener {
            override fun onItemClick(holder: NoteAdapter.ViewHolder, view: View, position: Int) {
                val item = adapter.getItem(position)

                Toast.makeText(context, "아이템 선택됨 : ${item?.contents}" , Toast.LENGTH_SHORT).show()
            }
        }) // 아이템 선택 됐을때 호출할 함수
    }
}