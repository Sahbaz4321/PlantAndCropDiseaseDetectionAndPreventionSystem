package com.example.ca2.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.ca2.data.firebase.FirebaseManager
import com.example.ca2.databinding.FragmentEditProfileBinding

class EditProfileFragment : Fragment() {

    private var _binding: FragmentEditProfileBinding? = null
    private val binding get() = _binding!!
    private val firebaseManager = FirebaseManager()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEditProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        loadUserData()

        binding.btnSave.setOnClickListener {
            saveUserData()
        }
    }

    private fun loadUserData() {
        val userId = firebaseManager.getCurrentUserId() ?: return
        firebaseManager.getUser(userId) { user ->
            if (isAdded && _binding != null) {
                user?.let {
                    binding.etName.setText(it.name)
                    binding.etEmail.setText(it.email)
                    binding.etPhone.setText(it.phoneNumber)
                }
            }
        }
    }

    private fun saveUserData() {
        val name = binding.etName.text.toString().trim()
        val phone = binding.etPhone.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()

        if (name.isEmpty()) {
            binding.tilName.error = "Name cannot be empty"
            return
        }

        val userId = firebaseManager.getCurrentUserId() ?: return
        
        firebaseManager.getUser(userId) { currentUser ->
            val updatedUser = currentUser?.copy(
                name = name,
                phoneNumber = phone
            ) ?: return@getUser
            
            // Update password if provided
            if (password.isNotEmpty()) {
                com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.updatePassword(password)
                    ?.addOnFailureListener {
                        Toast.makeText(requireContext(), "Failed to update password: ${it.message}", Toast.LENGTH_SHORT).show()
                    }
            }

            firebaseManager.saveUser(updatedUser) { success ->
                if (isAdded) {
                    if (success) {
                        Toast.makeText(requireContext(), "Profile updated successfully", Toast.LENGTH_SHORT).show()
                        findNavController().popBackStack()
                    } else {
                        Toast.makeText(requireContext(), "Failed to update profile", Toast.LENGTH_SHORT).show()
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
