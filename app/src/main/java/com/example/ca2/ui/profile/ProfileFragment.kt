package com.example.ca2.ui.profile

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.navOptions
import com.bumptech.glide.Glide
import com.example.ca2.R
import com.example.ca2.data.firebase.FirebaseManager
import com.example.ca2.databinding.FragmentProfileBinding

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private val firebaseManager = FirebaseManager()
    private var selectedImageUri: Uri? = null

    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = result.data?.data ?: return@registerForActivityResult
            uploadProfileImage(uri)
        }
    }

    private fun uploadProfileImage(uri: Uri) {
        val userId = firebaseManager.getCurrentUserId() ?: return
        
        binding.ivProfilePicture.alpha = 0.5f
        
        firebaseManager.uploadProfileImage(userId, uri, requireContext().contentResolver) { downloadUrl, errorMessage ->
            if (isAdded && _binding != null) {
                binding.ivProfilePicture.alpha = 1.0f
                if (downloadUrl != null) {
                    updateUserImageUrl(userId, downloadUrl)
                } else {
                    // Show the specific error from Firebase
                    Toast.makeText(context, "Error: $errorMessage", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun updateUserImageUrl(userId: String, url: String) {
        firebaseManager.getUser(userId) { user ->
            user?.let {
                val updatedUser = it.copy(profileImage = url)
                firebaseManager.saveUser(updatedUser) { success ->
                    if (isAdded && _binding != null) {
                        if (success) {
                            Glide.with(this).load(url).into(binding.ivProfilePicture)
                            Toast.makeText(context, "Profile updated!", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        loadUserData()

        // Profile Picture Change Logic
        binding.cvProfileImage.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            imagePickerLauncher.launch(intent)
        }

        binding.btnEditProfile.setOnClickListener {
            findNavController().navigate(R.id.action_profileFragment_to_editProfileFragment)
        }

        binding.menuFeedback.setOnClickListener {
            findNavController().navigate(R.id.action_profileFragment_to_feedbackFragment)
        }

        binding.menuSupport.setOnClickListener {
            findNavController().navigate(R.id.action_profileFragment_to_supportFragment)
        }

        binding.menuAbout.setOnClickListener {
            findNavController().navigate(R.id.action_profileFragment_to_aboutUsFragment)
        }

        binding.btnLogout.setOnClickListener {
            firebaseManager.logout()
            findNavController().navigate(R.id.loginFragment, null, navOptions {
                popUpTo(R.id.nav_graph) {
                    inclusive = true
                }
            })
        }
    }

    private fun loadUserData() {
        val userId = firebaseManager.getCurrentUserId() ?: return
        firebaseManager.getUser(userId) { user ->
            if (isAdded && _binding != null) {
                user?.let {
                    binding.tvUserName.text = it.name
                    binding.tvUserEmail.text = it.email
                    if (it.profileImage.isNotEmpty()) {
                        Glide.with(this)
                            .load(it.profileImage)
                            .placeholder(R.drawable.ic_app_logo)
                            .into(binding.ivProfilePicture)
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
