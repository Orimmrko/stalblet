package com.example.stalblet
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class OptionsBottomSheet : BottomSheetDialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(
        R.layout.fragment_options_bottom_sheet,
        container, false
    )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // MAP
        view.findViewById<Button>(R.id.btnOptionMap)
            .setOnClickListener {
                (activity as MainActivity).showMapFragment()
                dismiss()
            }

        // CHATS
        view.findViewById<Button>(R.id.btnOptionChats)
            .setOnClickListener {
                parentFragmentManager.beginTransaction()
                    .replace(R.id.auth_container, ChatListFragment())
                    .addToBackStack(null)
                    .commit()
                dismiss()
            }

        // MY LISTINGS
        view.findViewById<Button>(R.id.btnOptionListings)
            .setOnClickListener {
                parentFragmentManager.beginTransaction()
                    .replace(R.id.auth_container, MyListingsFragment())
                    .addToBackStack(null)
                    .commit()
                dismiss()
            }

        // ADD SUBLET
        view.findViewById<Button>(R.id.btnOptionAdd)
            .setOnClickListener {
                parentFragmentManager.beginTransaction()
                    .replace(R.id.auth_container, AddSubletFragment())
                    .addToBackStack(null)
                    .commit()
                dismiss()
            }

            // SIGN OUT (uses MainActivity.signOut())
            view.findViewById<Button>(R.id.btnOptionSignOut)
             .setOnClickListener {
                    (activity as MainActivity).signOut()
                    dismiss()
                 }
    }
}
