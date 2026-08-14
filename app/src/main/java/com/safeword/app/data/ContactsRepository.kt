package com.safeword.app.data

import kotlinx.coroutines.flow.Flow

class ContactsRepository(private val contactDao: ContactDao) {
    val allContacts: Flow<List<Contact>> = contactDao.getAllContacts()

    suspend fun getAllContactsDirect(): List<Contact> = contactDao.getAllContactsDirect()

    suspend fun insert(contact: Contact) {
        contactDao.insertContact(contact)
    }

    suspend fun delete(contact: Contact) {
        contactDao.deleteContact(contact)
    }
}
