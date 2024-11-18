package com.aryaman.myfamilyapp

import androidx.fragment.app.Fragment
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.aryaman.myfamilyapp.databinding.FragmentMapsBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.ola.mapsdk.interfaces.OlaMapCallback
import com.ola.mapsdk.model.OlaLatLng
import com.ola.mapsdk.model.OlaMarkerOptions
import com.ola.mapsdk.view.OlaMap
import com.ola.mapsdk.view.OlaMapView

class MapsFragment : Fragment() {

    // View Binding for fragment_maps.xml
    private var _binding: FragmentMapsBinding? = null
    private val binding get() = _binding!!
    val db = Firebase.firestore

    private lateinit var olaMapView: OlaMapView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout using View Binding
        _binding = FragmentMapsBinding.inflate(inflater, container, false)

        Log.d("MapsFragment", "onCreateView: called")
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        Log.d("MapsFragment", "onViewCreated: called")

        // Access the OlaMapView via binding
        olaMapView = binding.map

        // Initialize OlaMapView
        olaMapView.getMap(
            apiKey = R.string.ola_map_key.toString(), // Replace with your actual API key
            olaMapCallback = object : OlaMapCallback {
                override fun onMapReady(olaMap: OlaMap) {

                    displayInvitesOnMap(olaMap)
                }

                override fun onMapError(error: String) {
                    // Handle map error
                    // Log or display the error for debugging
                    Log.d("MapsFragment", "Map Error: $error")
                }
            }
        )
    }

    fun displayInvitesOnMap(olaMap: OlaMap) {
        val currentUser = FirebaseAuth.getInstance().currentUser

        // Ensure the user is logged in
        if (currentUser == null) {
            Log.e("MapsFragment", "User is not logged in.")
            return
        }

        val loggedInEmail = currentUser.email
        if (loggedInEmail.isNullOrEmpty()) {
            Log.e("MapsFragment", "Logged-in user email is null or empty.")
            return
        }


        // Access the "invites" collection for the logged-in user
        db.collection("users").document(loggedInEmail).collection("invites")
            .whereEqualTo("invite_status", 1)
            .get()
            .addOnSuccessListener { invitesSnapshot ->
                if (invitesSnapshot.isEmpty) {
                    Log.d("MapsFragment", "No invites with invite_status = 1 found.")
                    return@addOnSuccessListener
                }

                for (invite in invitesSnapshot) {
                    // Fetch the invite email
                    val inviteEmail = invite.id // The document ID represents the email

                    // Query the "users" collection for lat and long
                    db.collection("users").document(inviteEmail)
                        .get()
                        .addOnSuccessListener { userSnapshot ->
                            if (!userSnapshot.exists()) {
                                Log.e("MapsFragment", "User document for $inviteEmail does not exist.")
                                return@addOnSuccessListener
                            }

                            val lat = userSnapshot.get("lat")?.toString()?.toDoubleOrNull()
                            val long = userSnapshot.get("long")?.toString()?.toDoubleOrNull()

                            if (lat != null && long != null) {
                                // Add marker to the map
                                val markerOptions = OlaMarkerOptions.Builder()
                                    .setMarkerId("marker_$inviteEmail")
                                    .setPosition(OlaLatLng(lat, long))
                                    .setSnippet(inviteEmail)
                                    .setIsIconClickable(true)
                                    .setIconRotation(0f)
                                    .setIsAnimationEnable(true)
                                    .setIsInfoWindowDismissOnClick(true)
                                    .build()

                                olaMap.addMarker(markerOptions)

                                Log.d("MapsFragment", "Marker added for $inviteEmail.")
                            } else {
                                Log.e("MapsFragment", "Invalid lat/long for $inviteEmail.")
                            }
                        }
                        .addOnFailureListener { e ->
                            Log.e("MapsFragment", "Error fetching user document for $inviteEmail: ${e.message}")
                        }
                }
            }
            .addOnFailureListener { e ->
                Log.e("MapsFragment", "Error fetching invites: ${e.message}")
            }

        db.collection("users").document(loggedInEmail)
            .get()
            .addOnSuccessListener { userSnapshot ->
                if (!userSnapshot.exists()) {
                    Log.e("MapsFragment", "User document for $loggedInEmail does not exist.")
                    return@addOnSuccessListener
                }

                val lat = userSnapshot.get("lat")?.toString()?.toDoubleOrNull()
                val long = userSnapshot.get("long")?.toString()?.toDoubleOrNull()



                if (lat != null && long != null) {
                    // Add marker for current user
                    var location = OlaLatLng(lat, long)
                    val currentUserMarkerOptions = OlaMarkerOptions.Builder()
                        .setMarkerId("marker_current_user")
                        .setPosition(location)
                        .setSnippet("Me")
                        .setIsIconClickable(true)
                        .setIconRotation(0f)
                        .setIsAnimationEnable(true)
                        .setIsInfoWindowDismissOnClick(true)
                        .build()

                    olaMap.addMarker(currentUserMarkerOptions)

                    olaMap.zoomToLocation(location, 8.0)

                    Log.d("MapsFragment", "Marker added for current user.")
                } else {
                    Log.e("MapsFragment", "Invalid lat/long for current user.")
                }
            }
            .addOnFailureListener { e ->
                Log.e("MapsFragment", "Error fetching current user location: ${e.message}")
            }


    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null // Prevent memory leaks
    }

    companion object {

        @JvmStatic
        fun newInstance() = MapsFragment()
    }
}