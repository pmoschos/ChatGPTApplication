package pmoschos.chatgptapp.adapters;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Locale;

import pmoschos.chatgptapp.R;
import pmoschos.chatgptapp.helpers.SQLiteDBHelper;
import pmoschos.chatgptapp.models.Post;
import pmoschos.chatgptapp.utils.CustomAlertDialog;

public class PostAdapter extends RecyclerView.Adapter<PostAdapter.PostViewHolder> {

    private ArrayList<Post> originalPostArrayList;
    private ArrayList<Post> postArrayList;
    private Context context;
    private SQLiteDBHelper sqLiteDBHelper;

    public PostAdapter(ArrayList<Post> postArrayList, Context context) {
        this.originalPostArrayList = new ArrayList<>(postArrayList);
        this.postArrayList = postArrayList;
        this.context = context;
        this.sqLiteDBHelper = new SQLiteDBHelper(context);
    }

    @NonNull
    @Override
    public PostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_view, parent, false);
        return new PostViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PostViewHolder holder, @SuppressLint("RecyclerView") int position) {
        Post post = postArrayList.get(position);
        holder.questionTV.setText(post.getQuestion());
        holder.responseTV.setText(post.getResponse());

        holder.deleteTV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Get the position of the item to be deleted.
                int position = holder.getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    // The position is valid, show the confirmation dialog.
                    CustomAlertDialog customAlertDialog = new CustomAlertDialog(context);
                    customAlertDialog.yesPressed(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            // Perform the deletion if "Yes" is clicked.
                            Post postToDelete = postArrayList.get(position);
                            int rowsAffected = sqLiteDBHelper.deletePost(postToDelete.getPostId());
                            if (rowsAffected > 0) {
                                postArrayList.remove(position);
                                notifyItemRemoved(position);
                                notifyItemRangeChanged(position, postArrayList.size());
                                Toast.makeText(context, "Item deleted", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(context, "Error deleting post", Toast.LENGTH_SHORT).show();
                            }
                            customAlertDialog.dismiss();
                        }
                    });

                    customAlertDialog.noPressed(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            // Dismiss the dialog if "No" is clicked.
                            customAlertDialog.dismiss();
                        }
                    });

                    customAlertDialog.show();
                } else {
                    Toast.makeText(context, "Error: Position not found.", Toast.LENGTH_SHORT).show();
                }
            }
        });

    }

    @Override
    public int getItemCount() {
        return postArrayList.size();
    }

    public class PostViewHolder extends RecyclerView.ViewHolder {
        private TextView questionTV, responseTV, deleteTV;

        public PostViewHolder(@NonNull View itemView) {
            super(itemView);
            questionTV = itemView.findViewById(R.id.questionTV);
            responseTV = itemView.findViewById(R.id.responseTV);
            deleteTV = itemView.findViewById(R.id.deleteTV);
        }
    }

    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence charSequence) {
                String charString = charSequence.toString().toLowerCase(Locale.ROOT);
                if (charString.isEmpty()) {
                    postArrayList = new ArrayList<>(originalPostArrayList);
                } else {
                    ArrayList<Post> filteredList = new ArrayList<>();
                    for (Post row : originalPostArrayList) {
                        if (row.getQuestion().toLowerCase(Locale.ROOT).contains(charString) ||
                                row.getResponse().toLowerCase(Locale.ROOT).contains(charString)) {
                            filteredList.add(row);
                        }
                    }
                    postArrayList = filteredList;
                }
                FilterResults filterResults = new FilterResults();
                filterResults.values = postArrayList;
                return filterResults;
            }

            @Override
            protected void publishResults(CharSequence charSequence, FilterResults filterResults) {
                ArrayList<Post> potentialChildren = (ArrayList<Post>) filterResults.values;
                postArrayList = potentialChildren != null ? new ArrayList<>(potentialChildren) : new ArrayList<>();
                notifyDataSetChanged();
            }
        };
    }
}
