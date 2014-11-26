package com.ualberta.team17.view;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import com.ualberta.team17.AnswerItem;
import com.ualberta.team17.AttachmentItem;
import com.ualberta.team17.AuthoredTextItem;
import com.ualberta.team17.CommentItem;
import com.ualberta.team17.ItemType;
import com.ualberta.team17.QAModel;
import com.ualberta.team17.QuestionItem;
import com.ualberta.team17.R;
import com.ualberta.team17.UniqueId;
import com.ualberta.team17.controller.QAController;
import com.ualberta.team17.datamanager.DataFilter;
import com.ualberta.team17.datamanager.DataFilter.FilterComparison;
import com.ualberta.team17.datamanager.IIncrementalObserver;
import com.ualberta.team17.datamanager.IncrementalResult;
import com.ualberta.team17.datamanager.comparators.DateComparator;
import com.ualberta.team17.datamanager.comparators.IdComparator;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

public class QuestionViewActivity extends Activity implements IQAView {
	public final static String QUESTION_ID_EXTRA = "question_id";
	
	private QuestionItem mQuestion;
	private ArrayList<QABody> mQABodies;
	
	// Data for questions under construction
	private ArrayList<Bitmap> mImages;
	private LinearLayout mImageView;
	
	protected QAController mController;	
	protected QABodyAdapter mAdapter;
	
	private enum Mode {
		CREATE,
		DISPLAY
	}
	
	/**
	 * Constructor
	 */
	public QuestionViewActivity() {
		mQABodies = new ArrayList<QABody>();
		mImages = new ArrayList<Bitmap>();
	}
		
	/**
	 * Method that sets the question for mContent
	 * @author Joel
	 * @param question
	 */
	private void loadContent(QuestionItem question) {
		setQuestion(question);
		ListView listview = (ListView) findViewById(R.id.qaItemView);
		listview.setAdapter(createNewAdapter());
		IncrementalResult questionChildrenResult = mController.getChildren(question, new DateComparator());
		questionChildrenResult.addObserver(new AnswerResultListener(), ItemType.Answer);
		questionChildrenResult.addObserver(new CommentResultListener(), ItemType.Comment);		
	}
	
	/**
	 * Method that queries the controller for a question based on Id
	 * @author Joel
	 * @param id
	 */
	private void queryQuestion(UniqueId id) {		
		DataFilter dFilter = new DataFilter();
		dFilter.setTypeFilter(ItemType.Question);
		dFilter.addFieldFilter(QAModel.FIELD_ID, id.toString(), FilterComparison.EQUALS);
		IncrementalResult queryResult = mController.getObjects(dFilter, new IdComparator());
		//set up observer
		queryResult.addObserver(new QuestionResultListener(), ItemType.Question);
	}

	/**
	 * Inherited from IQAView
	 * 
	 * Updates the view when a model is changed.
	 * @param model The model that was changed.
	 */
	@Override
	public void update(QAModel model) {
		// we don't need to do any real work here, it will all happen in the adapter.
		refresh();
	}
	
	/**
	 * Refreshes the view.
	 * 
	 * Remakes the adapter so the view will redraw itself.
	 */
	private void refresh() {
		ListView qaList = (ListView) findViewById(R.id.qaItemView);
		mAdapter = new QABodyAdapter(this, R.id.qaItemView, mQABodies);
		qaList.invalidate();
		qaList.setAdapter(mAdapter);		
	}
		
	/**
	 * Initializes data depending on what is passed in the intent. Creates adapters and
	 * listeners for all data interactions that will happen.
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_questionview);
		
		View displayQuestionView = findViewById(R.id.displayQuestionView);
		View createQuestionView = findViewById(R.id.createQuestionView);
		
		Intent intent = this.getIntent();
		mController = QAController.getInstance();		
		
		((Button) displayQuestionView.findViewById(R.id.createAnswer)).setOnClickListener(new AddAnswerListener());		
		mAdapter = createNewAdapter();
		
		if (intent.getSerializableExtra(QUESTION_ID_EXTRA) != null) {
			setMode(Mode.DISPLAY);
			
			UniqueId id = UniqueId.fromString((String)intent.getSerializableExtra(QUESTION_ID_EXTRA));
			queryQuestion(id);			
		} else {
			setMode(Mode.CREATE);
			
			Button submitButton = (Button) createQuestionView.findViewById(R.id.createQuestionSubmitButton);
			EditText titleText = (EditText) createQuestionView.findViewById(R.id.createQuestionTitleView);
			EditText bodyText = (EditText) createQuestionView.findViewById(R.id.createQuestionBodyView);
			
			View attachmentsView = createQuestionView.findViewById(R.id.createQuestionAttachmentsView);
			ImageButton addAttachmentButton = (ImageButton) attachmentsView.findViewById(R.id.createQuestionAttachmentsAddButton);
			mImageView = (LinearLayout) attachmentsView.findViewById(R.id.createQuestionAttachmentsDisplayView);
			
			submitButton.setOnClickListener(new SubmitQuestionListener(titleText, bodyText));
			addAttachmentButton.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {
					addAttachment();					
				}
			});
		}
		
	}
	
	public void setMode(Mode mode) {
		View displayQuestionView = findViewById(R.id.displayQuestionView);
		View createQuestionView = findViewById(R.id.createQuestionView);
		
		if(mode == Mode.CREATE) {
			displayQuestionView.setVisibility(View.GONE);
			createQuestionView.setVisibility(View.VISIBLE);
		} else if(mode == Mode.DISPLAY) {
			displayQuestionView.setVisibility(View.VISIBLE);
			createQuestionView.setVisibility(View.GONE);
		}
	}
	

	public void favoriteQuestion(View v) {
		System.out.println("Favorite Question!");
		QAController.getInstance().addFavorite(getQuestion());
	}
	
	/**
	 * Getter for the QuestionItem.
	 * @return The question.
	 */
	public QuestionItem getQuestion() {
		return mQuestion;
	}
	
	/**
	 * Getter for the QABodies.
	 * @return The list of QABodies.
	 */
	public List<QABody> getQABodies() {
		return mQABodies;
	}
	
	/**
	 * Sets the question.
	 * 
	 * Also removes the old question from the list of QABodies and adds the new one.
	 * @param question The question to use.
	 */
	public void setQuestion(QuestionItem question) {
		resetContent();
		mQuestion = question;
		mQABodies.add(new QABody(question));
		refresh();
	}
	
	private void resetContent() {
		mQuestion = null;
		mQABodies = new ArrayList<QABody>();
	}
	
	/**
	 * Adds all answers passed to it to the QABody list
	 * 
	 * @param answers A list of AnswerItems.
	 */
	public void addAnswers(AnswerItem... answers) {
		for(AnswerItem answer : answers) {
			if (!exists(answer)) {
				answer.addView(this);
				QABody answerBody = new QABody(answer);
				mQABodies.add(answerBody);
			}			
		}
	}
	
	public boolean exists(AuthoredTextItem item) {
		for (QABody body : mQABodies) {
			if (body.parent.equals(item)) {
				return true;
			}			
		}
		return false;
	}
	
	/**
	 * Adds all comments to their corresponding parent
	 * Question/Answer by the id.
	 * 
	 * @param comments A list of CommentItems.
	 */
	public void addComments(CommentItem... comments) {
		for(CommentItem comment : comments) {
			QABody parentBody = findById(comment.getParentItem());
			if(parentBody != null) {
				if (!parentBody.comments.contains(comment)) {
					comment.addView(this);
					parentBody.comments.add(comment);
				}
			}
			else {
				// maybe some kind of error
			}
		}
	}
	
	/**
	 * Creates a new adapter for the list content.
	 * @return A new QABody adapter.
	 */
	public QABodyAdapter createNewAdapter() {
		return new QABodyAdapter(this, R.id.qaItemView, mQABodies);
	}
	
	/**
	 * Finds a QABody by its unique id. Returns null if not found.
	 * 
	 * @param id The id to search for.
	 * @return The matching QABody.
	 */
	private QABody findById(UniqueId id) {
		for(QABody body : mQABodies) {
			if(id.equals(body.parent.mUniqueId)){
				return body;
			}
		}
		return null;
	}
	
	private static final int IMAGE_REQUEST = 1888;
	
	private void addAttachment() {
		Intent imageIntent = new Intent();
		imageIntent.setType("image/*");
		imageIntent.setAction(Intent.ACTION_GET_CONTENT);
		imageIntent.addCategory(Intent.CATEGORY_OPENABLE);
		
		startActivityForResult(imageIntent, IMAGE_REQUEST);
	}
	
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if(requestCode == IMAGE_REQUEST && resultCode == RESULT_OK) {
			Bitmap bitmap = null;
			try {
				InputStream stream = getContentResolver().openInputStream(data.getData());
				bitmap = BitmapFactory.decodeStream(stream);
				stream.close();
				mImages.add(bitmap);
				addImage(bitmap);
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		super.onActivityResult(requestCode, resultCode, data);
	}
	
	private void addImage(Bitmap b) {
		LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		ImageView image = (ImageView) inflater.inflate(R.layout.attachment_image, mImageView, false);
		image.setImageBitmap(b);
		mImageView.addView(image);
	}
	
	/**
	 * This class holds a Question/Answer and its child Comments.
	 * 
	 * It's essentially a struct, so we just use public members.
	 * 
	 * @author Corey
	 *
	 */
	private class QABody {
		public AuthoredTextItem parent;
		public List<CommentItem> comments;
		
		public QABody(AuthoredTextItem initParent) {
			parent = initParent;
			comments = new ArrayList<CommentItem>();
		}
	}	
	
	/**
	 * Adapter for QABody. Connects the body of the Question/Answer
	 * with the bodyText field and Comments with the comments field.
	 * @author Corey + Joel
	 *
	 */
	private class QABodyAdapter extends ArrayAdapter<QABody> {
		Context mContext;
		List<QABody> mObjects;
		
		public QABodyAdapter(Context context, int textViewResourceId,
				List<QABody> objects) {
			super(context, textViewResourceId, objects);
			mContext = context;
			mObjects = objects;
		}		
		
		
		/**
		 * Returns the view after adding the list content.
		 */
		public View getView( int position, View convertView, ViewGroup parent ) {
			LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			View qaItemView = inflater.inflate(R.layout.qaitem, parent, false);
			View userBar = qaItemView.findViewById(R.id.userBar);
			
			TextView titleTextView = (TextView) qaItemView.findViewById(R.id.titleText);
			TextView bodyTextView = (TextView) qaItemView.findViewById(R.id.bodyText);
			TextView answerCountView = (TextView) qaItemView.findViewById(R.id.answerCountView);
			TextView authorTextView = (TextView) userBar.findViewById(R.id.authorText);
			
			ImageButton favoriteButton = (ImageButton) userBar.findViewById(R.id.favoriteButton);
			ImageButton attachmentButton = (ImageButton) userBar.findViewById(R.id.viewAttachmentButton);
			ImageButton commentButton = (ImageButton) userBar.findViewById(R.id.createCommentButton);
			ImageButton upvoteButton = (ImageButton) userBar.findViewById(R.id.upvoteButton);
			
			LinearLayout commentsView = (LinearLayout) qaItemView.findViewById(R.id.commentView);
			
			QABody qaItem = mObjects.get(position);
			if(qaItem.parent.mType == ItemType.Question) {
				QuestionItem question = (QuestionItem) qaItem.parent;
				
				titleTextView.setVisibility(View.VISIBLE);
				favoriteButton.setVisibility(View.VISIBLE);
				attachmentButton.setVisibility(View.VISIBLE);
				answerCountView.setVisibility(View.VISIBLE);
				
				titleTextView.setText(question.getTitle());
				if(question.getReplyCount() == 1) {
					answerCountView.setText(getString(R.string.answer_count_one));
				} else {
					answerCountView.setText(String.format(getString(R.string.answer_count), question.getReplyCount()));
				}
				
				if(question.isFavorited()) {
					favoriteButton.setImageResource(R.drawable.ic_action_important);
				} else {
					favoriteButton.setImageResource(R.drawable.ic_action_not_important);
				}
				favoriteButton.setOnClickListener(new FavoriteListener(question));
				
			} else if (qaItem.parent.mType == ItemType.Answer) {
				titleTextView.setVisibility(View.GONE);
				favoriteButton.setVisibility(View.GONE);
				attachmentButton.setVisibility(View.GONE);
				answerCountView.setVisibility(View.GONE);
				
			} else {
				// This should never happen. If it does, a bad object was added to the list.
				throw new IllegalStateException();
			}
						
			commentButton.setTag(qaItem.parent.getUniqueId());
			commentButton.setOnClickListener(new AddCommentListener(commentButton));
			
			upvoteButton.setOnClickListener(new UpvoteListener(qaItem.parent));
			
			bodyTextView.setText(qaItem.parent.getBody());
			authorTextView.setText(qaItem.parent.getAuthor());
			
			for (int i = 0; i < qaItem.comments.size(); i++){
				CommentItem comment = qaItem.comments.get(i);
				
				View commentView = inflater.inflate(R.layout.comment, parent, false);
				TextView commentBody = (TextView) commentView.findViewById(R.id.commentText);
				commentBody.setText(comment.getBody());
				
				TextView commentAuthor = (TextView) commentView.findViewById(R.id.commentAuthor);
				commentAuthor.setText("-" + comment.getAuthor());
				
				commentsView.addView(commentView);
			}					
			return qaItemView;
		}
	}
	
	/**
	 * Listener for an incremental result's question.
	 * @author Corey
	 *
	 */
	private class QuestionResultListener implements IIncrementalObserver {

		@Override
		public void itemsArrived(List<QAModel> item, int index) {
			loadContent((QuestionItem)item.get(0));			
		}
		
	}
	
	/**
	 * Listener for an incremental result's answers.
	 * @author Corey
	 *
	 */
	private class AnswerResultListener implements IIncrementalObserver {

		@Override
		public void itemsArrived(List<QAModel> item, int index) {				
			ListView qaList = (ListView) findViewById(R.id.qaItemView);
			for(QAModel qaitem : item ) {
				if (qaitem.mType == ItemType.Answer) {
					AnswerItem answer = (AnswerItem) qaitem;
					addAnswers(answer);
					IncrementalResult answerChildrenResult = mController.getChildren(answer, new DateComparator());
					answerChildrenResult.addObserver(new CommentResultListener(), ItemType.Comment);
				}
			}			
			qaList.invalidate();
			qaList.setAdapter(createNewAdapter());			
		}
		
	}
	
	/**
	 * Listener for an incremental result's comments.
	 * @author Corey
	 *
	 */
	private class CommentResultListener implements IIncrementalObserver {

		@Override
		public void itemsArrived(List<QAModel> item, int index) {				
			ListView qaList = (ListView) findViewById(R.id.qaItemView);
			List<CommentItem> comments = new ArrayList<CommentItem>();
			for(QAModel qaitem : item ) {				
				if (qaitem.mType == ItemType.Comment) {
					comments.add((CommentItem)qaitem);					
				}
			}			
			for (CommentItem comment : comments) {
				addComments(comment);
			}
			qaList.invalidate();
			qaList.setAdapter(createNewAdapter());			
		}
		
	}
	
	private class SubmitQuestionListener implements View.OnClickListener {
		EditText mTitleView;
		EditText mBodyView;

		public SubmitQuestionListener(EditText titleView, EditText bodyView) {
			mTitleView = titleView;
			mBodyView = bodyView;
		}
		@Override
		public void onClick(View v) {
			QAController controller = QAController.getInstance();
			setMode(Mode.DISPLAY);
			setQuestion(controller.createQuestion(mTitleView.getText().toString(), mBodyView.getText().toString()));
		}
		
	}
	
	private class AddAnswerListener implements View.OnClickListener {

		@Override
		public void onClick(View v) {
			AddAnswerPopup popup = new AddAnswerPopup(QuestionViewActivity.this);
			popup.show();
			
		}		
	}
	
	private class AddCommentListener implements View.OnClickListener {
		private View view;
		
		public AddCommentListener(View v) {
			super();
			view = v;
		}

		@Override
		public void onClick(View v) {
			AddCommentPopup popup = new AddCommentPopup(QuestionViewActivity.this, (UniqueId) view.getTag());	
			popup.show();
		}		
	}
	
	private class UpvoteListener implements View.OnClickListener {
		private QAModel mItem;
		
		public UpvoteListener(QAModel item) {
			mItem = item;
		}
		
		@Override
		public void onClick(View v) {
			QAController controller = QAController.getInstance(); 
			controller.upvote(mItem);
		}
		
	}
	
	private class FavoriteListener implements View.OnClickListener {
		private QuestionItem mItem;
		
		public FavoriteListener(QuestionItem item) {
			mItem = item;
		}
		
		@Override
		public void onClick(View v) {
			QAController controller = QAController.getInstance();
			controller.addFavorite(mItem);
		}
		
	}
	
	private class ViewAttachmentListener implements View.OnClickListener {

		@Override
		public void onClick(View v) {
			// TODO Auto-generated method stub
			
		}
		
	}
	
	private class AddAttachmentListener implements View.OnClickListener {
		@Override
		public void onClick(View v) {
			addAttachment();
		}
		
	}
	
	private class PopupCancelListener implements DialogInterface.OnClickListener {
		@Override
		public void onClick(DialogInterface dialog, int which) {
			//do nothing
		}
	}	
	
	private class AddAnswerPopup extends AlertDialog.Builder {
		private EditText answerBody;
		
		AddAnswerPopup (Context context) {
			super(context);
			answerBody = new EditText(context);	
			this.setTitle("Add an Answer");
			this.setView(answerBody);
			this.setPositiveButton("Submit", new AnswerPopupSubmitListener());
			this.setNegativeButton("Cancel", new PopupCancelListener());		
		}
		
		private class AnswerPopupSubmitListener implements DialogInterface.OnClickListener {					
			@Override
			public void onClick(DialogInterface dialog, int which) {
				String body = answerBody.getText().toString();
				AnswerItem newAnswer = mController.createAnswer(getQuestion(), body);								
				addAnswers(newAnswer);
				loadContent(getQuestion());
			}
		}		
	}
	
	private class AddCommentPopup extends AlertDialog.Builder {
		private EditText commentBody;	
		private UniqueId parentId;
		
		AddCommentPopup(Context context, UniqueId pId) {
			super(context);
			commentBody = new EditText(context);
			parentId = pId;
			this.setTitle("Add an Comment");
			this.setView(commentBody);
			this.setPositiveButton("Submit", new CommentPopupSubmitListener());
			this.setNegativeButton("Cancel", new PopupCancelListener());
		}
		
		private class CommentPopupSubmitListener implements DialogInterface.OnClickListener {					
			@Override
			public void onClick(DialogInterface dialog, int which) {
				String body = commentBody.getText().toString();
				CommentItem newComment = mController.createComment(parentId, body);								
				addComments(newComment);
				loadContent(getQuestion());			
			}
		}			
	}
	
	private class AddQuestionPopup extends AlertDialog.Builder {
		private LinearLayout layout;
		private EditText titleText;
		private EditText bodyText;		
		
		AddQuestionPopup(Context context) {
			super(context);
			layout = new LinearLayout(context);
			titleText = new EditText(context);
			bodyText = new EditText(context);
			layout.addView(titleText);
			layout.addView(bodyText);
			this.setTitle("New Question");
			this.setView(layout);
			this.setPositiveButton("add", new QuestionPopupSubmitListener());
			this.setNegativeButton("cancel", new PopupCancelListener());
		}
		
		private class QuestionPopupSubmitListener implements DialogInterface.OnClickListener {					
			@Override
			public void onClick(DialogInterface dialog, int which) {
				String title = titleText.getText().toString();
				String body = bodyText.getText().toString();
				QuestionItem newQuestion = mController.createQuestion(title, body);
				loadContent(newQuestion);
			}
		}	
	}
}
