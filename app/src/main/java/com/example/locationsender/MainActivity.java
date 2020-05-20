package com.example.locationsender;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

class CarName {
    private String name;
    private String contractAddress;

    public CarName() {}

    public CarName(String name, String contractAddress){
        this.name = name;
        this.contractAddress = contractAddress;
    }

    public void setContractAddress(String contractAddress){
        this.contractAddress = contractAddress;
    }

    public String getName() {
        return name;
    }

    public String getContractAddress() {
        return contractAddress;
    }

    @Override
    public String toString(){
        return getClass().getName() + "@" + Integer.toHexString(hashCode()) + " @name: " + getName() + " @contractAddress: " + getContractAddress();
    }
}


public class MainActivity extends AppCompatActivity {

    List<String> cars = new ArrayList<String>();
    List<CarName> carObjects = new ArrayList<CarName>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        final FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference ref = database.getReference("names");

        Toast.makeText(this, "Connecting to Firebase", Toast.LENGTH_SHORT).show();
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Object val = dataSnapshot.getValue();

                Iterable<DataSnapshot> children = dataSnapshot.getChildren();
                for(DataSnapshot x: children){
                    CarName car;
                    car = null;
                    // System.out.println(x.toString());
                    // System.out.println("x.hasChildren(): " + x.hasChildren());
                    if(x.hasChildren()){
                        /* The next line will create a CarName object and map all the values into it. */
                        car = x.getValue(CarName.class);

                        assert car != null;
                        car.setContractAddress(x.getKey());
                    }
                    System.out.println(car);

                    // Add the current car object into the cars array and carObjects array.
                    assert car != null;
                    cars.add(car.getName());
                    carObjects.add(car);
                }

//                assert val != null;
//                Toast.makeText(MainActivity.this, val.toString(), Toast.LENGTH_SHORT).show();


                ListView listView = (ListView) findViewById(R.id.car_list);

                final ArrayAdapter<String> adapter = new ArrayAdapter<String>(MainActivity.this, R.layout.car_list_layout, cars);

                listView.setAdapter(adapter);

                listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    public void onItemClick(AdapterView parent, View v, int position, long id) {
                        // Do something in response to the click
                        String value = adapter.getItem(position);
//                        Toast.makeText(getApplicationContext(),
//                                carObjects.get(position).getContractAddress()
//                                        + ": " + carObjects.get(position).getName(),
//                                Toast.LENGTH_SHORT).show();

//                      Car Selected, Find location and send to firebase.
                        Intent intent = new Intent(MainActivity.this, GetUserLocation.class);
                        intent.putExtra("contractAddress", carObjects.get(position).getContractAddress());
                        intent.putExtra("carName", carObjects.get(position).getName());
                        startActivity(intent);
                    }
                });

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }
}

