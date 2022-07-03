package com.ishant.passwordmanager.ui.activities.create_edit_view_password_activity.fragments

import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.PopupMenu
import android.widget.Toast
import androidx.core.view.children
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.deishelon.roundedbottomsheet.RoundedBottomSheetDialog
import com.google.android.material.chip.Chip
import com.google.android.material.snackbar.Snackbar
import com.ishant.passwordmanager.R
import com.ishant.passwordmanager.adapters.LogoCompanyChooserAdapter
import com.ishant.passwordmanager.adapters.PasswordAccountInfoAdapter
import com.ishant.passwordmanager.databinding.BottomSheetOptionsBinding
import com.ishant.passwordmanager.databinding.CompanyChooserSheetBinding
import com.ishant.passwordmanager.databinding.FragmentCreatePasswordBinding
import com.ishant.passwordmanager.databinding.FragmentEditPasswordBinding
import com.ishant.passwordmanager.db.entities.EncryptedKey
import com.ishant.passwordmanager.db.entities.Entry
import com.ishant.passwordmanager.db.entities.EntryDetail
import com.ishant.passwordmanager.security.EncryptionDecryption
import com.ishant.passwordmanager.ui.activities.create_edit_view_password_activity.CreateEditViewPasswordActivity
import com.ishant.passwordmanager.ui.activities.password_activity.PasswordActivity
import com.ishant.passwordmanager.ui.viewmodels.CreateEditViewPasswordViewModel
import com.ishant.passwordmanager.util.CompanyList
import com.ishant.passwordmanager.util.CompanyListData
import com.ishant.passwordmanager.util.Passwords
import kotlinx.coroutines.*
import java.util.*


class EditPasswordFragment : Fragment(R.layout.fragment_edit_password) {
    private lateinit var binding: FragmentEditPasswordBinding
    private lateinit var adapter: PasswordAccountInfoAdapter
    lateinit var viewModel: CreateEditViewPasswordViewModel
    lateinit var accountDetailList: MutableList<EntryDetail>
    val args: EditPasswordFragmentArgs by navArgs()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentEditPasswordBinding.bind(view)
        val mBottomSheetDialog = RoundedBottomSheetDialog(requireContext())
        val sheetView = layoutInflater.inflate(R.layout.bottom_sheet_options, null)
        mBottomSheetDialog.setContentView(sheetView)
        val sheetBinding: BottomSheetOptionsBinding = BottomSheetOptionsBinding.bind(sheetView)
        val data = args.data
        binding.entryTitleLayout.editText?.setText(data.title)
        when(data.category) {
            "Social" -> binding.chipSocial.isChecked = true
            "Mails" -> binding.chipMail.isChecked = true
            "Cards" -> binding.chipCards.isChecked = true
            "Work" -> binding.chipWork.isChecked = true
            "Other" -> binding.chipOther.isChecked = true
            else -> binding.chipWork.isChecked = true
        }
        viewModel = (activity as CreateEditViewPasswordActivity).viewModel
        accountDetailList = mutableListOf<EntryDetail>()
        val securityClass = EncryptionDecryption()
        val rvAccountDetails = binding.rvAccountDetails
        rvAccountDetails.layoutManager = LinearLayoutManager(requireContext())
       CoroutineScope(Dispatchers.IO).launch {
           val oldEntryDetailList = viewModel.getAllEntryDetailsOneTime(data.id)
           for(i in 0..oldEntryDetailList.size-1) {
               val oldEntryDetailKey = viewModel.getAllEncryptedKeysOneTime(oldEntryDetailList[i].id)[0]
               val decryptedData = securityClass.decrypt(
                   oldEntryDetailList[i].detailContent,
                   oldEntryDetailKey.emdKey,
                   securityClass.getKey()
               )
               val decryptedEntryDetail = EntryDetail(oldEntryDetailList[i].id,oldEntryDetailList[i].entryId,oldEntryDetailList[i].detailType,decryptedData)
               accountDetailList.add(decryptedEntryDetail)
           }
           withContext(Dispatchers.Main) {
               adapter = PasswordAccountInfoAdapter(accountDetailList)
               rvAccountDetails.adapter = adapter
           }
       }

        val itemTouchHelper = ItemTouchHelper(object: ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP or ItemTouchHelper.DOWN,0) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                val startPos = viewHolder.adapterPosition
                val endPos = target.adapterPosition
                Collections.swap(accountDetailList, startPos, endPos)
                adapter.notifyItemMoved(startPos, endPos)
                return true
            }
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {

            }
        })
        itemTouchHelper.attachToRecyclerView(binding.rvAccountDetails)
        var companyIcon = data.icon
        binding.btnBack.setOnClickListener {
            val intent = Intent(requireContext(), PasswordActivity::class.java)
            startActivity(intent)
            (activity as CreateEditViewPasswordActivity).finish()
        }
        binding.btnIcon.setOnClickListener {
            val iBottomSheetDialog = RoundedBottomSheetDialog(requireContext())
            val sheetView = layoutInflater.inflate(R.layout.company_chooser_sheet, null)
            iBottomSheetDialog.setContentView(sheetView)
            val companySheetBinding: CompanyChooserSheetBinding = CompanyChooserSheetBinding.bind(
                sheetView
            )
            val companyList = CompanyListData.companyListData
            val companyAdapter = LogoCompanyChooserAdapter(companyList)
            companySheetBinding.rvCompanyChooser.adapter = companyAdapter
            companySheetBinding.rvCompanyChooser.layoutManager = LinearLayoutManager(requireContext())
            companySheetBinding.rvCompanyChooser.isNestedScrollingEnabled = true;
            iBottomSheetDialog.show()
            companyAdapter.setOnItemClickListener {
                companyIcon = it.id
                Snackbar.make(view, "${it.companyName}'s logo selected", Snackbar.LENGTH_SHORT).show()
                iBottomSheetDialog.dismiss()
            }
            iBottomSheetDialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
            companySheetBinding.searchBar.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                }
                override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                    val companyListData = CompanyListData.companyListData
                    val filteredList = mutableListOf<CompanyList>()
                    if (p0 != null) {
                        for (company in companyListData) {
                            if (company.companyName.contains(p0)) {
                                filteredList.add(company)
                            }
                        }
                    }
                    val filteredCompanyAdapter = LogoCompanyChooserAdapter(filteredList)
                    companySheetBinding.rvCompanyChooser.adapter = filteredCompanyAdapter
                }
                override fun afterTextChanged(p0: Editable?) {
                }
            })
        }

        binding.btnNewEntry.setOnClickListener {
            val popupMenu = PopupMenu(requireContext(), it)
            popupMenu.menuInflater.inflate(R.menu.account_details_menu, popupMenu.menu)
            popupMenu.show()
            popupMenu.setOnMenuItemClickListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.miUsername -> {
                        showBottomSheet(mBottomSheetDialog,sheetBinding,0)
                        popupMenu.dismiss()
                    }
                    R.id.miEmail -> {
                        showBottomSheet(mBottomSheetDialog,sheetBinding,1)
                        popupMenu.dismiss()
                    }
                    R.id.miPhone -> {
                        showBottomSheet(mBottomSheetDialog,sheetBinding,2)
                        popupMenu.dismiss()
                    }
                    R.id.miPassword -> {
                        showBottomSheet(mBottomSheetDialog,sheetBinding,3)
                        popupMenu.dismiss()
                    }
                    R.id.miWebsite -> {
                        showBottomSheet(mBottomSheetDialog,sheetBinding,4)
                        popupMenu.dismiss()
                    }
                    R.id.miNote -> {
                        showBottomSheet(mBottomSheetDialog,sheetBinding,5)
                        popupMenu.dismiss()
                    }
                }
                true
            }
        }


        binding.btnSave.setOnClickListener {
            val entryTitle = binding.entryTitleLayout.editText?.text.toString()
            val entryCategory: String = (binding.categoryChipGroup.children.toList().filter {
                (it as Chip).isChecked
            }[0] as Chip).text.toString()
            val entryIcon = companyIcon
            val entryDetailsList = accountDetailList
            if(entryTitle.isNotEmpty() || entryTitle.isNotBlank()) {
                if(entryCategory.isNotEmpty() || entryCategory.isNotBlank()) {
                    if(entryDetailsList.isNotEmpty()) {
                        /* val dialog = SpotsDialog.Builder()
                             .setContext(requireContext())
                             .setMessage("Encrypting and Saving your Details")
                             .setCancelable(false)
                             .build(
                         dialog.show()*/
                        val dialog = ProgressDialog.show(requireContext(), "Saving", "Please wait, we are encrypting and saving all your information", true, false)
                        dialog.show()
                        val password1 = Passwords.PASSWORD1
                        val password2 = Passwords.PASSWORD2
                        CoroutineScope(Dispatchers.IO).launch {
                            // Deleting Old Data
                            val oldEntryDetailList = viewModel.getAllEntryDetailsOneTime(data.id)
                            for(i in 0..oldEntryDetailList.size-1) {
                                viewModel.deleteEncryptedKeys(oldEntryDetailList[i].id)
                            }
                            viewModel.deleteEntryDetails(data.id)
                            // Adding New Data
                            val entry = Entry(data.id, entryTitle, entryCategory, entryIcon, data.favourite)
                            val id = async { viewModel.upsertEntry(entry) }.await()
                            for(entryDetail in entryDetailsList) {
                                val encryptedObject = securityClass.encrypt(
                                    entryDetail.detailContent,
                                    password1,
                                    securityClass.getKey()
                                )
                                val encryptedData = encryptedObject.encryptedData
                                val emdKey = encryptedObject.key
                                entryDetail.id = 0
                                entryDetail.entryId = id
                                entryDetail.detailContent = encryptedData
                                val entryDetailId = async { viewModel.upsertEntryDetail(
                                    entryDetail
                                ) }.await()
                                val keyObject = EncryptedKey(0, entryDetailId, emdKey)
                                async { viewModel.upsertEncryptedKey(keyObject) }.await()
                            }
                            withContext(Dispatchers.Main) {
                                dialog.dismiss()
                                val intent = Intent(requireContext(), PasswordActivity::class.java)
                                startActivity(intent)
                                (activity as CreateEditViewPasswordActivity).finish()
                            }
                        }
                    } else {
                        Snackbar.make(
                            view,
                            "You must add at least one detail about your account",
                            Snackbar.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    Snackbar.make(view, "You must select a category", Snackbar.LENGTH_SHORT).show()
                }
            } else {
                Snackbar.make(view, "Title cannot be blank", Snackbar.LENGTH_SHORT).show()
            }
        }
    }




    private fun showBottomSheet(mBottomSheetDialog: RoundedBottomSheetDialog, sheetBinding: BottomSheetOptionsBinding, optionType: Int) {
        var detailType = ""
        var detailContent = ""
        sheetBinding.optionInputLayout.editText?.text?.clear()
        sheetBinding.optionInputLayout.editText?.clearFocus()
        when (optionType) {
            0 -> {
                detailType = "Username"
                sheetBinding.optionInputLayout.helperText = "Eg. user710"
                sheetBinding.optionInputLayout.editText?.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_NORMAL
            }
            1 -> {
                detailType = "Email"
                sheetBinding.optionInputLayout.helperText = "Eg. user@example.com"
                sheetBinding.optionInputLayout.editText?.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_NORMAL
            }
            2 -> {
                detailType = "Phone Number"
                sheetBinding.optionInputLayout.editText?.inputType =
                    InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL or InputType.TYPE_NUMBER_FLAG_SIGNED or InputType.TYPE_CLASS_PHONE
                sheetBinding.optionInputLayout.helperText = "Eg. +91 9876012345"
            }
            3 -> {
                detailType = "Password"
                sheetBinding.optionInputLayout.isPasswordVisibilityToggleEnabled = true
                sheetBinding.optionInputLayout.helperText = "Always keep strong passwords"
                sheetBinding.optionInputLayout.editText?.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            }
            4 -> {
                detailType = "Website"
                sheetBinding.optionInputLayout.helperText = "Eg. www.example.com"
                sheetBinding.optionInputLayout.editText?.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_NORMAL
            }
            5 -> {
                detailType = "Notes"
                sheetBinding.optionInputLayout.editText?.minLines = 3
                sheetBinding.optionInputLayout.helperText = "You can add some notes or details here"
                sheetBinding.optionInputLayout.editText?.inputType =
                    InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
            }
        }

        sheetBinding.optionInputLayout.hint = detailType
        mBottomSheetDialog.show()
        sheetBinding.btnAddOption.setOnClickListener {
            val validateMessage = validateInput(
                sheetBinding.optionInputLayout.editText?.text.toString(),
                optionType
            )
            if(validateMessage=="Validated") {
                val accountDetailObj = EntryDetail(
                    1,
                    1,
                    detailType,
                    sheetBinding.optionInputLayout.editText?.text.toString()
                )
                accountDetailList.add(accountDetailObj)
                adapter.notifyDataSetChanged()
                mBottomSheetDialog.dismiss()
            } else {
                sheetBinding.optionInputLayout.error = validateMessage
            }
        }

    }

    private fun validateInput(input: String, type: Int): String {
        when (type) {
            0 -> {
                // Validate for Username
                if (nullCheckInput(input)) {
                    return "Validated"
                } else {
                    return "You must fill username"
                }
            }
            1 -> {
                // Validate for Email
                if (nullCheckInput(input)) {
                    if (input.contains("@") && input.contains(".")) {
                        return "Validated"
                    } else {
                        return "Incorrect Email Format"
                    }
                } else {
                    return "Username cannot be blank"
                }
            }
            2 -> {
                // Validate for Phone Number
                if (nullCheckInput(input)) {
                    if (input.length > 2) {
                        return "Validated"
                    } else {
                        return "Phone number should be more than two digits"
                    }
                } else {
                    return "Phone number cannot be blank"
                }
            }
            3 -> {
                // Validate for Password
                if (nullCheckInput(input)) {
                    return "Validated"
                } else {
                    return "Password cannot be blank"
                }
            }
            4 -> {
                // Validate for Website
                if (nullCheckInput(input)) {
                    if (input.contains(".")) {
                        return "Validated"
                    } else {
                        return "Incorrect Website Format"
                    }
                } else {
                    return "Website URL cannot be blank"
                }
            }
            5 -> {
                // Validate for Note
                if (nullCheckInput(input)) {
                    return "Validated"
                } else {
                    return "Note cannot be blank"
                }
            }
            else -> return "An Error Occurred"
        }
    }

    private fun nullCheckInput(input: String): Boolean {
        return input.isNotEmpty() && input.isNotBlank()
    }


}