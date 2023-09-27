package org.altmail.dicttextviewlistener;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.LinkedList;

/**
 * Created by rom on 04/02/18.
 */

public class ParcelableLinkedList implements Parcelable {

    private final LinkedList<String> linkedList;

    public final Creator<ParcelableLinkedList> CREATOR = new Creator<ParcelableLinkedList>() {

        @Override
        public ParcelableLinkedList createFromParcel(Parcel in) {

            return new ParcelableLinkedList(in);
        }

        @Override
        public ParcelableLinkedList[] newArray(int size) {
            return new ParcelableLinkedList[size];
        }

    };

    private ParcelableLinkedList(Parcel in) {
        // Read size of list
        final int size = in.readInt();
        // Read the list
        linkedList = new LinkedList<>();

        for (int i = 0; i < size; i++) {

            linkedList.add(in.readString());
        }

    }

    ParcelableLinkedList(LinkedList<String> linkedList) {
        this.linkedList = linkedList;
    }

    LinkedList<String> getLinkedList() {
        return linkedList;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags) {
        // Write size of the list
        parcel.writeInt(linkedList.size());
        // Write the list
        for (String entry : linkedList) {

            parcel.writeString(entry);
        }
    }
}
