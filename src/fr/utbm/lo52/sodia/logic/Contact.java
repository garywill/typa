package fr.utbm.lo52.sodia.logic;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import android.content.ContentProviderOperation;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.net.Uri;
import android.os.RemoteException;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.RawContacts;

public class Contact extends DataBaseObject implements InterGroup<Group>
{

	private static Map<Long, Contact> contacts;

	private Uri lookup;

	private String name;
	private List<RawContact> rawContacts;
	private Set<Group> groups;

	protected Uri uri = ContactsContract.Contacts.CONTENT_URI;
	protected String[] projection = new String[]
		{ ContactsContract.Contacts._ID, ContactsContract.Contacts.DISPLAY_NAME };

	protected Contact(Uri lookup)
	{
		super(getIdFromUri(lookup));
		this.lookup = lookup;
	}

	protected Contact(long id)
	{
		super(id);
	}

	public Contact(String name)
	{
		rawContacts = new ArrayList<RawContact>();
		groups = new HashSet<Group>();
		this.name = name;
	}

	@Override
	public void add(Group group)
	{
		if (!groups.contains(group))
		{
			groups.add(group);
			group.add(this);
		}

	}

	@Override
	public void remove(Group group)
	{
		if (groups.contains(group))
		{
			groups.remove(group);
			group.remove(this);
		}
	}

	public void add(Set<Group> groups)
	{
		if (!this.groups.containsAll(groups))
		{
			this.groups.addAll(groups);
			for (Group group : groups)
			{
				group.add(this);
			}
		}
	}

	public void remove(Set<Group> groups)
	{
		for (Group group : groups)
		{
			remove(group);
		}
	}

	public String getName()
	{
		return this.name;
	}

	public void setName(String name)
	{
		this.name = name;
	}

	public void addRawContact(RawContact rawContact)
	{
		this.rawContacts.add(rawContact);
	}

	public void removeRawContact(RawContact rawContact)
	{
		this.rawContacts.remove(rawContact);
	}

	public Uri getLookup()
	{
		return this.lookup;
	}

	public Set<Group> getGroups()
	{
		return this.groups;
	}

	public static long getIdFromUri(Uri uri)
	{
		final Cursor contact = contentResolver.query(uri, new String[]
			{ Contacts._ID }, null, null, null);
		contact.moveToFirst();
		return contact.getLong(0);
	}

	@Override
	protected void get()
	{
		rawContacts = new ArrayList<RawContact>();
		groups = new HashSet<Group>();
		contacts.put(id, this);
		final Cursor cursor = query();
		if (cursor != null && cursor.moveToFirst())
		{
			setName(cursor.getString(1));
		}
		if (cursor != null)
		{
			cursor.close();
		}
		// RawContact Fill
		final Cursor rawContacts = (new RawContact(false, null, null)).query(
				RawContacts.CONTACT_ID + "=?", new String[]
					{ Long.toString(id) });
		while (rawContacts != null && rawContacts.moveToNext())
		{
			addRawContact(new RawContact(cursor.getLong(0)));
		}
		if (rawContacts != null)
		{
			rawContacts.close();
		}
		// Group Fill
		final Cursor groups = contentResolver
				.query(ContactsContract.Data.CONTENT_URI,
						new String[]
							{ ContactsContract.CommonDataKinds.GroupMembership.GROUP_ROW_ID },
						ContactsContract.CommonDataKinds.GroupMembership.CONTACT_ID
								+ "=?", new String[]
							{ Long.toString(id) }, null);
		while (groups.moveToNext())
		{
			this.add(Group.get(groups.getLong(0)));
		}
	}

	public static Contact get(long id)
	{
		if (contacts.containsKey(id))
		{
			return contacts.get(id);
		}
		return new Contact(id);
	}

	@Override
	public void save() throws RemoteException, OperationApplicationException
	{
		for (RawContact rawContact : this.rawContacts)
		{
			rawContact.setName(name);
		}
		save((DataBaseObject[]) this.rawContacts.toArray());
		save((DataBaseObject[]) this.groups.toArray());
	}

	@Override
	public boolean equals(Object object)
	{
		Contact contact = (Contact) object;
		return contact.name == this.name || super.equals(object);
	}

	@Override
	protected ContentProviderOperation operation()
	{
		return null;
	}

}
