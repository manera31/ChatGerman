package com.joanmanera.firebasechat;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity implements IChatMessageListener{

   private static final int SIGN_IN_REQUEST_CODE = 1001;
   private FirebaseFirestore db;
   private CollectionReference chatRef;
   private ChatMessageAdapter adapter;
   private String username;
   private final String TAG = MainActivity.class.getSimpleName();
   private RecyclerView recyclerView;

   @Override
   protected void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      setContentView(R.layout.activity_main);

      // Choose authentication providers
      List<AuthUI.IdpConfig> providers = Arrays.asList(
              new AuthUI.IdpConfig.EmailBuilder().build(),
              new AuthUI.IdpConfig.PhoneBuilder().build(),
              new AuthUI.IdpConfig.GoogleBuilder().build(),
              new AuthUI.IdpConfig.FacebookBuilder().build(),
              new AuthUI.IdpConfig.TwitterBuilder().build());

      if(FirebaseAuth.getInstance().getCurrentUser() == null) {
         // Iniciamos Activity para Login/Registro
         startActivityForResult(
               AuthUI.getInstance()
                     .createSignInIntentBuilder()
                       .setAvailableProviders(providers)
                       .build(),
               SIGN_IN_REQUEST_CODE
         );
      } else {
         // El usuario ya se ha autenticado.
         Toast.makeText(this,
               "Bienvenido " + FirebaseAuth.getInstance().getCurrentUser().getDisplayName(), Toast.LENGTH_LONG).show();

         setupFirestore();
      }

      FloatingActionButton fab = (FloatingActionButton)findViewById(R.id.fab);

      fab.setOnClickListener(new View.OnClickListener() {
         @Override
         public void onClick(View view) {
            EditText etMessage = (EditText)findViewById(R.id.etMessage);
            String message = etMessage.getText().toString();
            ChatMessage chatMessage = new ChatMessage(username, message);

            chatRef.add(chatMessage)
                  .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                     @Override
                     public void onSuccess(DocumentReference documentReference) {
                        // Si el mensaje se ha enviado correctamente

                     }
                  })
                  .addOnFailureListener(new OnFailureListener() {
                     @Override
                     public void onFailure(@NonNull Exception e) {
                        // Si algo ha fallado
                     }
                  });
            // Limpiamos el campo de texto
            etMessage.setText("");
         }
      });
   }

   public void displayChatMessages() {

   }

   @Override
   protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
      super.onActivityResult(requestCode, resultCode, data);

      if(requestCode == SIGN_IN_REQUEST_CODE) {
         if(resultCode == RESULT_OK) {
            Toast.makeText(this,
                  "Acceso autorizado. ¡Bienvenido!",
                  Toast.LENGTH_LONG)
                  .show();

            setupFirestore();
            displayChatMessages();
         } else {
            Toast.makeText(this,
                  "Acceso denegado. Inténtalo de nuevo más tarde.",
                  Toast.LENGTH_LONG)
                  .show();

            // Close the app
            finish();
         }
      }
   }

   public void setupFirestore() {
      username = FirebaseAuth.getInstance().getCurrentUser().getDisplayName();
      db = FirebaseFirestore.getInstance();
      chatRef = db.collection("FirebaseChat");

      Query query = chatRef.orderBy("time", Query.Direction.ASCENDING);

      FirestoreRecyclerOptions<ChatMessage> options = new FirestoreRecyclerOptions.Builder<ChatMessage>()
            .setQuery(query, ChatMessage.class)
            .build();

      // Si necesitamos personalizar como el modelo es parseado podemos utilizar la clase SnapshotParser
      /*
         ...setQuery(..., new SnapshotParser<Chat>() {
             @NonNull
             @Override
             public Chat parseSnapshot(@NonNull DocumentSnapshot snapshot) {
                 return ...;
             }
         });

       */

      adapter = new ChatMessageAdapter(options, this);
      adapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
         @Override
         public void onItemRangeInserted(int positionStart, int itemCount) {
            super.onItemRangeInserted(positionStart, itemCount);
            recyclerView.smoothScrollToPosition(adapter.getItemCount()-1);
         }
      });
      recyclerView = findViewById(R.id.rvMessages);
      recyclerView.setLayoutManager(new LinearLayoutManager(this));
      recyclerView.setAdapter(adapter);


      Log.d(TAG, "Start listeninng");
      adapter.startListening();
   }

   @Override
   protected void onStop() {
      super.onStop();
      if(adapter != null) {
         Log.d(TAG, "Stop listening");
         adapter.stopListening();
      }
   }

   @Override
   public boolean onCreateOptionsMenu(Menu menu) {
      getMenuInflater().inflate(R.menu.main_menu, menu);
      return true ;
   }

   @Override
   public boolean onOptionsItemSelected(@NonNull MenuItem item) {
      if(item.getItemId() == R.id.menu_sign_out) {
         AuthUI.getInstance().signOut(this)
               .addOnCompleteListener(new OnCompleteListener<Void>() {
                  @Override
                  public void onComplete(@NonNull Task<Void> task) {
                     Toast.makeText(MainActivity.this,
                           "Te has desconectado.",
                           Toast.LENGTH_LONG)
                           .show();

                     // Close activity
                     finish();
                  }
               });
      }
      return true;
   }

   @Override
   public void onChatMessageListener(int chatMessage) {
      Toast.makeText(this, String.valueOf(chatMessage), Toast.LENGTH_SHORT).show();
   }
}
