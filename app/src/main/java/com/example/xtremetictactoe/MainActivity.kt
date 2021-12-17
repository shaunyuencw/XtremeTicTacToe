package com.example.xtremetictactoe

import android.annotation.SuppressLint
import android.content.DialogInterface
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.InputFilter
import android.text.Spanned
import android.util.Log
import android.view.*
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.view.menu.MenuBuilder
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import org.w3c.dom.CharacterData
import org.w3c.dom.Text

data class User(
    val displayName: String = "",
    val emojis: String = ""
)

class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

class MainActivity : AppCompatActivity() {
    private companion object {
        private const val TAG = "MainActivity"
        private val VALID_CHAR_TYPES = listOf(
            Character.NON_SPACING_MARK, // 6
            Character.DECIMAL_DIGIT_NUMBER, // 9
            Character.LETTER_NUMBER, // 10
            Character.OTHER_NUMBER, // 11
            Character.SPACE_SEPARATOR, // 12
            Character.FORMAT, // 16
            Character.SURROGATE, // 19
            Character.DASH_PUNCTUATION, // 20
            Character.START_PUNCTUATION, // 21
            Character.END_PUNCTUATION, // 22
            Character.CONNECTOR_PUNCTUATION, // 23
            Character.OTHER_PUNCTUATION, // 24
            Character.MATH_SYMBOL, // 25
            Character.CURRENCY_SYMBOL, //26
            Character.MODIFIER_SYMBOL, // 27
            Character.OTHER_SYMBOL // 28
        ).map { it.toInt() }.toSet()
    }

    private lateinit var auth: FirebaseAuth
    private lateinit var rvUsers: RecyclerView

    // Access a Cloud Firestore instance from your Activity
    val db = Firebase.firestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        auth = Firebase.auth

        rvUsers = findViewById(R.id.rvUsers)

        val query = db.collection("users")
        val options = FirestoreRecyclerOptions.Builder<User>().setQuery(query, User::class.java)
            .setLifecycleOwner(this).build()

        val adapter = object: FirestoreRecyclerAdapter<User, UserViewHolder>(options) {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
                val view = LayoutInflater.from(this@MainActivity).inflate(android.R.layout.simple_list_item_2, parent, false)
                return UserViewHolder(view)
            }

            override fun onBindViewHolder(holder: UserViewHolder, position: Int, model: User) {
                val tvName: TextView = holder.itemView.findViewById(android.R.id.text1)
                val tvEmojis: TextView = holder.itemView.findViewById(android.R.id.text2)

                tvName.text = model.displayName
                tvEmojis.text = model.emojis
            }
        }
        rvUsers.adapter = adapter
        rvUsers.layoutManager = LinearLayoutManager(this)
        Toast.makeText(this, "MainActivity", Toast.LENGTH_SHORT)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.miLogout) {
            Log.i(TAG, "Logout")

            // Logout the user
            auth.signOut()

            val logoutIntent = Intent(this, LoginActivity::class.java)
            logoutIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK

            startActivity(logoutIntent)
        }

        else if (item.itemId == R.id.miEditStatus) {
            Log.i(TAG, "Show alert dialog to edit status")

            dialogEditEmoji()
        }

        else if (item.itemId == R.id.miEditName) {
            Log.i(TAG, "Show alert dialog to edit display name")

            dialogEditName()
        }

        return super.onOptionsItemSelected(item)
    }

    inner class EmojiFilter : InputFilter {
        override fun filter(source: CharSequence?, p1: Int, p2: Int, p3: Spanned?, p4: Int, p5: Int): CharSequence {
            // If the passes the filter, return source, else return ""

            if (source == null || source.isBlank()) {
                return ""
            }
            Log.i(TAG, "Added text $source, it has length ${source.length} characters")
            for (inputChar in source) {
                val type = Character.getType(inputChar)
                Log.i(TAG, "Character type $type")
                if (!VALID_CHAR_TYPES.contains(type)) {
                    Toast.makeText(this@MainActivity, "Only emojis are allowed", Toast.LENGTH_SHORT).show()
                    return ""
                }
            }

            // Valid!
            return source
        }
    }

    private fun dialogEditEmoji() {
        val editText = EditText(this)
        // Restrict input length and only to emojis
        val emojiFilter = EmojiFilter()
        var lengthFilter = InputFilter.LengthFilter(9) // 2-4 Unicode Char
        editText.filters = arrayOf(lengthFilter, emojiFilter)

        var dialog = AlertDialog.Builder(this)
            .setTitle("Update your emojis")
            .setView(editText)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("OK", null)
            .show()
        dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener {
            Log.i(TAG, "Clicked on positive button")

            val emojisEntered = editText.text.toString()
            if (emojisEntered.isBlank()) {
                Toast.makeText(this, "Cannot submit empty text", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val currentUser: FirebaseUser? = auth.currentUser
            if (currentUser == null) {
                Toast.makeText(this, "No signed in user", Toast.LENGTH_SHORT).show()
            }

            // Update Firestore with the new emojis
            db.collection("users").document(currentUser!!.uid)
                .update("emojis", emojisEntered)
            dialog.dismiss()
        }
    }

    private fun dialogEditName() {
        val editText = EditText(this)
        // Restrict input length and only to emojis
        var lengthFilter = InputFilter.LengthFilter(20) // 2-4 Unicode Char
        editText.filters = arrayOf(lengthFilter)

        var dialog = AlertDialog.Builder(this)
            .setTitle("Display Name")
            .setView(editText)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("OK", null)
            .show()
        dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener {
            Log.i(TAG, "Clicked on positive button")

            val newDisplayName = editText.text.toString()
            if (newDisplayName.isBlank()) {
                Toast.makeText(this, "Cannot submit empty text", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val currentUser: FirebaseUser? = auth.currentUser
            if (currentUser == null) {
                Toast.makeText(this, "No signed in user", Toast.LENGTH_SHORT).show()
            }

            // Update Firestore with the new emojis
            db.collection("users").document(currentUser!!.uid)
                .update("displayName", newDisplayName)
            dialog.dismiss()
        }
    }
}