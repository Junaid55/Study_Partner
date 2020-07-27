package com.studypartner.fragments;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.studypartner.R;
import com.studypartner.models.FileItem;
import com.studypartner.utils.FileType;

import java.io.File;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import static android.content.Context.MODE_PRIVATE;

public class HomeFragment extends Fragment {
	private static final String TAG = "HomeFragment";
	
	private File noteFolder;
	
	private ArrayList<FileItem> starred = new ArrayList<>();
	private ArrayList<FileItem> links = new ArrayList<>();
	private ArrayList<FileItem> notes = new ArrayList<>();
	
	public HomeFragment() {}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
	                         Bundle savedInstanceState) {
		Log.d(TAG, "onCreateView: starts");
		
		return inflater.inflate(R.layout.fragment_home, container, false);
	}
	
	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		
		FirebaseUser firebaseUser= FirebaseAuth.getInstance().getCurrentUser();
		
		if (firebaseUser != null && firebaseUser.getEmail() != null) {
			File studyPartnerFolder = new File(String.valueOf(Objects.requireNonNull(Objects.requireNonNull(Objects.requireNonNull(Objects.requireNonNull(requireContext().getExternalFilesDir(null)).getParentFile()).getParentFile()).getParentFile()).getParentFile()), "StudyPartner");
			if (!studyPartnerFolder.exists()) {
				if (studyPartnerFolder.mkdirs()) {
					noteFolder = new File(studyPartnerFolder, firebaseUser.getEmail());
				} else {
					noteFolder = new File(requireContext().getExternalFilesDir(null), firebaseUser.getEmail());
				}
			} else {
				noteFolder = new File(studyPartnerFolder, firebaseUser.getEmail());
			}
		} else {
			File studyPartnerFolder = new File(String.valueOf(Objects.requireNonNull(Objects.requireNonNull(Objects.requireNonNull(Objects.requireNonNull(requireContext().getExternalFilesDir(null)).getParentFile()).getParentFile()).getParentFile()).getParentFile()), "StudyPartner");
			if (!studyPartnerFolder.exists()) {
				if (studyPartnerFolder.mkdirs()) {
					noteFolder = studyPartnerFolder;
				} else {
					noteFolder = requireContext().getExternalFilesDir(null);
				}
			} else {
				noteFolder = studyPartnerFolder;
			}
		}
	}
	
	private void populateDataAndSetAdapter() {
		
		SharedPreferences starredPreference = requireActivity().getSharedPreferences(FirebaseAuth.getInstance().getCurrentUser().getUid() + "STARRED", MODE_PRIVATE);
		
		if (starredPreference.getBoolean("STARRED_ITEMS_EXISTS", false)) {
			Gson gson = new Gson();
			String json = starredPreference.getString("STARRED_ITEMS", "");
			Type type = new TypeToken<List<FileItem>>() {}.getType();
			starred = gson.fromJson(json, type);
			
			if (starred == null) starred = new ArrayList<>();
		}
		
		ArrayList<FileItem> starredToBeRemoved = new ArrayList<>();
		for (FileItem starItem : starred) {
			File starFile = new File(starItem.getPath());
			if (starItem.getType() == FileType.FILE_TYPE_LINK) {
				if (starFile.getParentFile() == null || !starFile.getParentFile().exists()) {
					starredToBeRemoved.add(starItem);
				}
			} else {
				if (!starFile.exists()) {
					starredToBeRemoved.add(starItem);
				}
			}
		}
		starred.removeAll(starredToBeRemoved);
		
		SharedPreferences linkPreference = requireActivity().getSharedPreferences(FirebaseAuth.getInstance().getCurrentUser().getUid() + "LINK", MODE_PRIVATE);
		
		if (linkPreference.getBoolean("LINK_ITEMS_EXISTS", false)) {
			Gson gson = new Gson();
			String json = linkPreference.getString("LINK_ITEMS", "");
			Type type = new TypeToken<List<FileItem>>() {}.getType();
			links = gson.fromJson(json, type);
			
			if (links == null) links = new ArrayList<>();
		}
		
		ArrayList<FileItem> linkToBeRemoved = new ArrayList<>();
		for (FileItem linkItem : links) {
			File linkParent = new File(linkItem.getPath()).getParentFile();
			assert linkParent != null;
			if (!linkParent.exists()) {
				linkToBeRemoved.add(linkItem);
			}
		}
		links.removeAll(linkToBeRemoved);
		
		notes = new ArrayList<>();
		
		addRecursively(noteFolder);
		
		notes.addAll(links);
		
		Collections.sort(notes, new Comparator<FileItem>() {
			@Override
			public int compare(FileItem o1, FileItem o2) {
				return o1.getDateModified().compareTo(o2.getDateModified());
			}
		});
		
		
	}
	
	private void addRecursively(File folder) {
		FileItem item = new FileItem(folder.getPath());
		if (folder.isDirectory()) {
			for (File file: Objects.requireNonNull(folder.listFiles())) {
				addRecursively(file);
			}
		} else {
			if (isStarred(item)) {
				item.setStarred(true);
			}
			notes.add(item);
		}
	}
	
	private boolean isStarred (FileItem item) {
		if (starred != null) {
			for (int i = 0; i < starred.size(); i++) {
				FileItem starredItem = starred.get(i);
				if (starredItem.getPath().equals(item.getPath())) {
					return true;
				}
			}
		}
		return false;
	}
}