package com.example.myfamilyapp

import android.content.Context
import android.os.Bundle
import android.provider.ContactsContract
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.CompletableFuture

class HomeFragment : Fragment() {

    lateinit var mContext: Context
    private val listContacts: ArrayList<ContactModel> = ArrayList()
    lateinit var inviteAdapter: InviteAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mContext = context
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val listMembers = listOf<MemberModel>(
            MemberModel(
                "Lokesh",
                "9th buildind, 2nd floor, maldiv road, manali 9th buildind, 2nd floor",
                "90%",
                "220"
            ),
            MemberModel(
                "Kedia",
                "10th buildind, 3rd floor, maldiv road, manali 10th buildind, 3rd floor",
                "80%",
                "210"
            ),
            MemberModel(
                "D4D5",
                "11th buildind, 4th floor, maldiv road, manali 11th buildind, 4th floor",
                "70%",
                "200"
            ),
            MemberModel(
                "Ramesh",
                "12th buildind, 5th floor, maldiv road, manali 12th buildind, 5th floor",
                "60%",
                "190"
            ),
            MemberModel(
                "Ramesh",
                "12th buildind, 5th floor, maldiv road, manali 12th buildind, 5th floor",
                "60%",
                "190"
            ),
            MemberModel(
                "Ramesh",
                "12th buildind, 5th floor, maldiv road, manali 12th buildind, 5th floor",
                "60%",
                "190"
            ),
            MemberModel(
                "Ramesh",
                "12th buildind, 5th floor, maldiv road, manali 12th buildind, 5th floor",
                "60%",
                "190"
            ),
        )

        val adapter = MemberAdapter(listMembers)

        val recycler = requireView().findViewById<RecyclerView>(R.id.recycler_member)
        recycler.layoutManager = LinearLayoutManager(requireContext())
        recycler.adapter = adapter

        inviteAdapter = InviteAdapter(listContacts)

        // initializes a live observer on the DB, which notifies the inviteAdapter on any changes in the DB
        fetchDatabaseContacts()

        CoroutineScope(Dispatchers.IO).launch {
            // fetches data from phone's repository and inserts it into the DB
            insertDatabaseContacts(fetchContacts())
        }

        val recyclerInvite = requireView().findViewById<RecyclerView>(R.id.recycler_invite)
        recyclerInvite.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        recyclerInvite.adapter = inviteAdapter

        // logout on click of three dots
        val threeDots = requireView().findViewById<ImageView>(R.id.icon_three_dots)
        threeDots.setOnClickListener {
            SharedPref.putBoolean(PrefConstants.IS_USER_LOGGED_IN, false)
            FirebaseAuth.getInstance().signOut()
        }
    }

    private fun fetchContacts(): ArrayList<ContactModel> {

        val cr = mContext.contentResolver
        val cursor = cr.query(ContactsContract.Contacts.CONTENT_URI,
            null, null, null, null)

        val listContacts: ArrayList<ContactModel> = ArrayList()

        if (cursor != null && cursor.count > 0) {

            while (cursor.moveToNext()) {
                val idIndex = cursor.getColumnIndex(ContactsContract.Contacts._ID)
                val nameIndex = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)
                val hasPhoneNumberIndex = cursor.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER)

                val id = if (idIndex != -1) cursor.getString(idIndex) else null
                val name = if (nameIndex != -1) cursor.getString(nameIndex) else null
                val hasPhoneNumber = if (hasPhoneNumberIndex != -1) cursor.getInt(hasPhoneNumberIndex) else 0

                if (hasPhoneNumber > 0 && id != null) {
                    val pCur = cr.query(
                        ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                        null,
                        ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                        arrayOf(id),
                        ""
                    )

                    pCur?.use {
                        if (it.moveToFirst()) {
                            val phoneNumIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                            do {
                                val phoneNum = if (phoneNumIndex != -1) it.getString(phoneNumIndex) else null
                                if (phoneNum != null && name != null) {
                                    listContacts.add(ContactModel(name, phoneNum))
                                }
                            } while (it.moveToNext())
                        }
                    }
                }
            }

            cursor.close()

        }
        return listContacts
    }

    private fun fetchDatabaseContacts() {
        val database = MyFamilyDatabase.getDatabase(mContext)
        val contactDao = database.contactDao()

        contactDao.getAllContacts().observe(viewLifecycleOwner) {
            listContacts.clear()
            listContacts.addAll(it)
            inviteAdapter.notifyDataSetChanged()
        }

    }

    private fun insertDatabaseContacts(listContacts: ArrayList<ContactModel>) {
        val database = MyFamilyDatabase.getDatabase(mContext)

        CompletableFuture.runAsync {
            database.contactDao().insertAll(listContacts)
        }
    }

    companion object {

        @JvmStatic
        fun newInstance() = HomeFragment()
    }
}