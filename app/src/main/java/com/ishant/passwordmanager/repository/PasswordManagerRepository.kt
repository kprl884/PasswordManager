package com.ishant.passwordmanager.repository

import com.ishant.passwordmanager.db.PasswordManagerDatabase
import com.ishant.passwordmanager.db.entities.EncryptedKey
import com.ishant.passwordmanager.db.entities.Entry
import com.ishant.passwordmanager.db.entities.EntryDetail

class PasswordManagerRepository(val db: PasswordManagerDatabase) {
    suspend fun upsertEntry(entry: Entry): Long = db.getPasswordManagerDao().upsertEntry(entry)

    suspend fun deleteEntry(entry: Entry) = db.getPasswordManagerDao().deleteEntry(entry)

    fun getAllEntries() = db.getPasswordManagerDao().getAllEntries()

    suspend fun upsertEntryDetail(entryDetail: EntryDetail) = db.getPasswordManagerDao().upsertEntryDetail(entryDetail)

    suspend fun deleteEntryDetails(id: Int) = db.getPasswordManagerDao().deleteEntryDetails(id)

    fun getAllEntryDetails(id: Int) = db.getPasswordManagerDao().getAllEntryDetails(id)

    suspend fun upsertEncryptedKey(encryptedKey: EncryptedKey) = db.getPasswordManagerDao().upsertKey(encryptedKey)

    suspend fun deleteEncryptedKeys(id: Int) = db.getPasswordManagerDao().deleteKeys(id)

    fun getAllEncryptedKey(id: Int) = db.getPasswordManagerDao().getAllEncryptedKeys(id)




}