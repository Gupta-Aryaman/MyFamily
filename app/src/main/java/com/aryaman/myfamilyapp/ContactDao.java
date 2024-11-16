package com.aryaman.myfamilyapp;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface ContactDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(ContactModel contact);

    @Query("SELECT * FROM ContactModel")
    LiveData<List<ContactModel>> getAllContacts();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<ContactModel> contactModelList);
}
