package com.docuvantage.andhelper.main;
//==================================================
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.RemoteException;
import android.os.StrictMode;
import android.provider.MediaStore;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.webkit.MimeTypeMap;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.docuvantage.andhelper.hessian.DVClientHelper;
import com.docuvantage.andhelper.hessian.DVWebApiClient;
import com.docuvantage.andhelper.util.EmbedUtil;
import com.dv.edm.api.bean.DArchive;
import com.dv.edm.api.constants.Constants;
import com.dv.edm.api.exception.DException;

public class DocuVantageLogin extends Activity implements OnClickListener {

	public String appUrl = "https://ltp3.docuvantage.com/";
	
    public Button login_but; 
    
    public String username;
    public String password;
    
    public Button btnCancel;
    public Button btnLogin;
    public EditText txtUsername;
    public EditText txtPassword;
    public CheckBox rememberMe;

    private Button btnLoginDialog;
    private WebView webView;
    public static final int progress_bar_type = 0;
    public ProgressDialog progressDialog;

    public static final String MyPREFERENCES = "MyPrefs";
    public static final String UserNameKey = "usernameKey"; 
    public static final String PasswordKey = "passwordKey";
    public static final String ChkRememberKey = "chkKey";
    public static final String FLAT = "flatKey"; 
    public boolean chk;
    public boolean flag = false;
    public SharedPreferences sharedpreferences;
    public static final String MY_PREFS = "SharedPreferences";
        
    private TextView name;
    private TextView pass;
    public String n, p, c;
    public Uri receivedUri;
    public String[] archive111 = new String[0];
    public int index = 0;
    boolean shareFlag = false;
    public static DVClientHelper dv = new DVClientHelper();
    public Context context;
    public String mimeType = "";
    public long archive_id;
    public String the_uri;    
    
    boolean intentFlag = false;
    
    @SuppressLint("SetJavaScriptEnabled")
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		
		Intent receivedIntent = getIntent();
		String receivedAction = receivedIntent.getAction();
    	String receivedType = receivedIntent.getType();

    	if(receivedAction.equals(Intent.ACTION_SEND)) {
    		receivedUri = (Uri)receivedIntent.getParcelableExtra(Intent.EXTRA_STREAM);//file:///storage/emulated/0/Download/Test%20Document.docx               content://media/external/images/media/31103
			//check we have a uri
			if (receivedUri != null) {
					
					String webSessionId = new String(dv.getSessionId().getKey());
					try {
						shareToApp(appUrl, webSessionId, dv, receivedUri);
						
					} catch (Exception e) {
						
						e.printStackTrace();
						System.out.println(e.getMessage());
					}

			}
    		
    	} else if(receivedAction.equals(Intent.ACTION_SEND_MULTIPLE)&& receivedIntent.hasExtra(Intent.EXTRA_STREAM)){

    			String webSessionId = new String(dv.getSessionId().getKey());
    			try {
					shareToAppMultipleFiles(appUrl, webSessionId, dv, receivedUri);
				} catch (Exception e) {
					e.printStackTrace();
				}

    	} 
    	else if(receivedAction.equals(Intent.ACTION_VIEW)){
    		receivedUri = (Uri)receivedIntent.getParcelableExtra(Intent.EXTRA_STREAM);//file:///storage/emulated/0/Download/Test%20Document.docx
    		
    		String scheme = receivedIntent.getScheme();
            ContentResolver resolver = getContentResolver();
            
            if (scheme.compareTo(ContentResolver.SCHEME_CONTENT) == 0) {
                Uri uri = receivedIntent.getData();
                String name = getContentName(resolver, uri);

                Log.v("tag" , ""+receivedIntent.getDataString() + " : " + receivedIntent.getType() + " : " + name);
                InputStream input = null;
				try {
					input = resolver.openInputStream(uri);
				} catch (FileNotFoundException e) {
					Log.e("1", e.getMessage());
					e.printStackTrace();
				}

                String importfilepath = "/sdcard/My Documents/" + name; 
                InputStreamToFile(input, importfilepath);
                String webSessionId = new String(dv.getSessionId().getKey());
                try {
					shareToApp(appUrl, webSessionId, dv, receivedUri);
				} catch (Exception e) {
					Log.e("2", e.getMessage());
					e.printStackTrace();
				}
            }
            
			if (receivedUri != null) {
					String webSessionId = new String(dv.getSessionId().getKey());
					try {
						shareToApp(appUrl, webSessionId, dv, receivedUri);
						
					} catch (Exception e) {
						Log.e("3", e.getMessage());
						e.printStackTrace();
						System.out.println(e.getMessage());
					}

			}
    	}
    	else {
    	
			flag = true;	
			sharedpreferences = getSharedPreferences(MyPREFERENCES, 0);
			if (!sharedpreferences.contains(UserNameKey)){
				Editor editor = sharedpreferences.edit();		
				editor.putString(UserNameKey, "");
				editor.putString(PasswordKey, "");
				editor.putBoolean(ChkRememberKey, false);
				editor.putBoolean(FLAT, false);
				editor.commit();
				
			} 
			else {
				n = sharedpreferences.getString(UserNameKey, "");
				p = sharedpreferences.getString(PasswordKey, "");
				
			}
			showResult();
    	
    	}
	}
    private void InputStreamToFile(InputStream in, String file) {
        try {
            OutputStream out = new FileOutputStream(new File(file));
            int size = 0;
            byte[] buffer = new byte[1024];
            while ((size = in.read(buffer)) != -1) {
                out.write(buffer, 0, size);
            }
            out.close();
        }
        catch (Exception e) {
        	Log.e("4", e.getMessage());
        }
    }
    private String getContentName(ContentResolver resolver, Uri uri){
        Cursor cursor = resolver.query(uri, null, null, null, null);
        cursor.moveToFirst();
        int nameIndex = cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME);
        if (nameIndex >= 0) {
            return cursor.getString(nameIndex);
        } else {
            return null;
        }
    }
    public static DArchive[] printArchiveList(DVClientHelper dv) throws DException {
    	
        DArchive[] archives = dv.getEngine().listArchives(Constants.ACTION_ARCHIVE_CREATERECORD);
        System.out.println("Can upload to these archives:");
        System.out.println("--------------");
        for (DArchive archive : archives) {
            if (archive.isAllowObjects()) {
                System.out.println("" + archive.getId() + " " + archive.getName());
            }
        }
        System.out.println("--------------");
        return archives;
    }
    public void shareToAppMultipleFiles(final String appUrl, final String webSessionId, final DVClientHelper dv, Uri theUri) throws Exception {
    	 shareFlag = true;
         sharedpreferences = getSharedPreferences(MyPREFERENCES, 0);
         boolean loginFlag = sharedpreferences.getBoolean(FLAT, false);
         if(!loginFlag){
         	intentFlag = true;
         	showLogin();
         	return;
         }
         if (android.os.Build.VERSION.SDK_INT > 9) {
             StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
             StrictMode.setThreadPolicy(policy);
         }
         
         final DArchive[] archives = dv.getEngine().listArchives(Constants.ACTION_ARCHIVE_CREATERECORD);
         final String[] archive111 = new String[archives.length];
         final long[] archive222 = new long[archives.length];
  
         for (DArchive archive : archives) {
             if (archive.isAllowObjects()) {
             	System.out.println("" + archive.getId() + " " + archive.getName());
             	
             	String archiveName = archive.getName();              	
             	long archive_id =  archive.getId();
             	archive222[index] = archive_id; 
             	archive111[index] = archiveName;
             	index++;
             }
         }
         
        
         AlertDialog.Builder builder = new AlertDialog.Builder(this);  
 	     builder.setTitle("Upload To Archive");  
 	     
 	     builder.setItems(archive111, new DialogInterface.OnClickListener() {  
 	       @Override  
 	       public void onClick(DialogInterface dialog, int which) {  
 	    	   String link = null;
 	    	   
 	    	   String folderName = "Folder";
 	    	   long archiveId = archive222[which]; 
 	    	   long parentId = dv.getEngine().folderCreate(folderName, archiveId);//it will always be one file 	           
 	           
 		    	try {
 		    		ArrayList list = getIntent().getParcelableArrayListExtra(Intent.EXTRA_STREAM);
 		        	for (Object p : list) {
 		    	    	Uri uri = (Uri) p; /// do something with it.
 		    	    	Log.e("photomover",uri.toString());
 		    	
 		    	    	String convert_url = uri.toString().substring(uri.toString().lastIndexOf("/")+1);
 		    	        
 		    	        if(isNumeric(convert_url)){
 		    	        	the_uri = getRealPathFromURI(getApplicationContext(), uri);
 		    	        } else {
 		    	        	the_uri = uri.getPath();
 		    	        }
 		    	        
 		    	        final File file = new File(the_uri);
 		    	        final String filePath = file.toString();
 		    	        int startIndex = filePath.lastIndexOf("/");
 		    	        final String fileName = filePath.substring(startIndex+1);
 		    	    	
 		    	    	String mimeType = getMimeType(filePath);

 		    	    	try {
 		    	    		uploadFile(dv, 0, parentId, archiveId, mimeType, filePath, fileName);
 		    	    		dv.getEngine().documentClose(Constants.DOCUMENT_MODE_WRITE, parentId, true);
 		 		        	try {
								link = EmbedUtil.makeCreateRecordUrl(appUrl, webSessionId, archiveId, parentId, folderName, Constants.OBJECT_TYPE_FILE);
							} catch (Exception e) {
								e.printStackTrace();
							}
// 		    	    		
 							webView = (WebView)findViewById(R.id.newWebView);
 		    	    	}
 		    	    	catch (IOException e) {
 		    	    		Log.e("photomover", e.toString());
 		    	    	}
 		    	    	
 		        	}
 		        	
 		        	System.out.println("Create Record link = " + link);
 		        	startWebView(link);
 		    		
 				} catch (DException e) {
 					e.printStackTrace();
					System.out.println(e.getMessage());
 					Toast.makeText(getApplicationContext(), ""+e.getMessage().toString(), Toast.LENGTH_LONG).show();
 				}
 		        dialog.dismiss();  

 	       }  
 	     });  
 	     builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {  
 	           @Override  
 	           public void onClick(DialogInterface dialog, int which) {  
 	             dialog.dismiss(); 
 	             DocuVantageLogin.this.finish();
 	           }  
 	         });  
 	     AlertDialog alert = builder.create();  
 	     alert.show();  
    	
    }
    
	public void shareToApp(final String appUrl, final String webSessionId, final DVClientHelper dv, Uri theUri) throws Exception {
        
        shareFlag = true;
        sharedpreferences = getSharedPreferences(MyPREFERENCES, 0);
        boolean loginFlag = sharedpreferences.getBoolean(FLAT, false);
        if(!loginFlag){
        	intentFlag = true;
        	showLogin();
        	return;
        }
        if (android.os.Build.VERSION.SDK_INT > 9) {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }
        
        final DArchive[] archives = dv.getEngine().listArchives(Constants.ACTION_ARCHIVE_CREATERECORD);
        final String[] archive111 = new String[archives.length];
        final long[] archive222 = new long[archives.length];
 
        for (DArchive archive : archives) {
            if (archive.isAllowObjects()) {
            	System.out.println("" + archive.getId() + " " + archive.getName());
            	
            	String archiveName = archive.getName();              	
            	long archive_id =  archive.getId();
            	archive222[index] = archive_id; 
            	archive111[index] = archiveName;
            	index++;
            }
        }
        String convert_url = theUri.toString().substring(theUri.toString().lastIndexOf("/")+1);
        
        if(isNumeric(convert_url)){
        	the_uri = getRealPathFromURI(getApplicationContext(), theUri);
        } else {
        	the_uri = theUri.getPath();
        }
        
        final File file = new File(the_uri);
        final String filePath = file.toString();
        int startIndex = filePath.lastIndexOf("/");
        final String fileName = filePath.substring(startIndex+1);
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this);  
	     builder.setTitle("Upload To Archive");  
	     
	     builder.setItems(archive111, new DialogInterface.OnClickListener() {  
	       @Override  
	       public void onClick(DialogInterface dialog, int which) {  
	    	   String link = null;
	    	   
	    	   long parentId = 0;//it will always be one file
	           long archiveId = archive222[which]; // this is just picking the first archive, it should use the one the user selected
	           String mimeType = getMimeType(filePath);
	           
		    	try {
		    		File new_upload_file = new File(filePath);
		    		if(!new_upload_file.isFile()){
		    			Toast.makeText(getApplicationContext(), "This is not supported", Toast.LENGTH_LONG).show();
		    		}
		    		long new_upload_file_size = new_upload_file.length();
		    		
		    		if(new_upload_file_size == 0){
		    			Toast.makeText(getApplicationContext(), "Cannot upload empty file", Toast.LENGTH_LONG).show();
		    		} else {
		    			//uploadFile(dv, openObjectId, parentId, archiveId, mimeType, filePath, file.getName());
		    			long newobjectId = uploadFile(dv, 0, parentId, archiveId, mimeType, filePath, fileName);
		    			
						try {
							
							link = EmbedUtil.makeCreateRecordUrl(appUrl, webSessionId, archiveId, newobjectId, fileName, Constants.OBJECT_TYPE_FILE);
							webView = (WebView)findViewById(R.id.newWebView);
							startWebView(link);
							
						} catch (Exception e) {
							e.printStackTrace();
							System.out.println(e.getMessage());
						}
						
				        System.out.println("Create Record link = " + link);
		    		}
				} catch (DException e) {
					e.printStackTrace();
					System.out.println(e.getMessage());
					Toast.makeText(getApplicationContext(), ""+e.getMessage().toString(), Toast.LENGTH_LONG).show();
				} catch (IOException e) {
					e.printStackTrace();
					System.out.println(e.getMessage());
					Toast.makeText(getApplicationContext(), ""+e.getMessage().toString(), Toast.LENGTH_LONG).show();
				}
		        dialog.dismiss();  

	       }  
	     });  
	     builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {  
	           @Override  
	           public void onClick(DialogInterface dialog, int which) {  
	             dialog.dismiss(); 
	             DocuVantageLogin.this.finish();
	           }  
	         });  
	     AlertDialog alert = builder.create();  
	     alert.show();  
    }
	public String getMimeType(String url)
	{
		String type = null;
 	   	if(url.lastIndexOf(".") != -1) {
 	   		String ext = url.substring(url.lastIndexOf(".")+1);
 	   		MimeTypeMap mime = MimeTypeMap.getSingleton();
 	   		type = mime.getMimeTypeFromExtension(ext);
 	   } else {
 		   type = null;
 	   }
		return type;

	}
	public void startWebView(String _url) {

		webView.setWebViewClient(new WebViewClient() {      
	        ProgressDialog progressDialog;
	        public boolean shouldOverrideUrlLoading(WebView view, String _url) {
	        	if(_url.startsWith("docuvantageondemand://logout")){
	        		try {
	    				dv.disconnect();
	    				DocuVantageLogin.this.finish();
	    			} catch (RemoteException e) {
	    				e.printStackTrace();
						System.out.println(e.getMessage());
	    			}
	        	}
	        	if(_url.startsWith("docuvantageondemand://goback")){
	        		DocuVantageLogin.this.finish();
	        	}
	            view.loadUrl(_url);
	        	return true;
	        }
	    
	        public void onLoadResource (WebView view, String url) {
	            if (progressDialog == null) {
	                progressDialog = new ProgressDialog(DocuVantageLogin.this);
	                progressDialog.setMessage("Loading. Please wait...");
	                progressDialog.setCanceledOnTouchOutside(false);
	                progressDialog.show();
	            } 
	        }
	        public void onPageFinished(WebView view, String url) {
	        	super.onPageFinished(view, url);
	            try{
	                if (progressDialog.isShowing()) {
	                    progressDialog.dismiss();

	                }
	                
	            }catch(Exception e){
	            	e.printStackTrace();
					System.out.println(e.getMessage());
	            }
	        }
	        
	         
	    }); 
		
		webView.getSettings().setJavaScriptEnabled(true); 	    
		webView.getSettings().setLoadWithOverviewMode(true);
		webView.getSettings().setUseWideViewPort(true);
		webView.setScrollBarStyle(WebView.SCROLLBARS_OUTSIDE_OVERLAY);
		webView.setScrollbarFadingEnabled(false);
		webView.getSettings().setBuiltInZoomControls(true);
		webView.getSettings().setDisplayZoomControls(false);
	    
		webView.loadUrl(_url);
		
	    
	}
	

	
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case progress_bar_type:
			progressDialog = new ProgressDialog(this);
			progressDialog.setMessage("Downloading file. Please wait...");
			progressDialog.setIndeterminate(false);
			progressDialog.setMax(100);
			progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
			progressDialog.setCancelable(true);
			progressDialog.show();
			return progressDialog;
		default:
			return null;
		}
	}
	long objectIdg;
	
	public long uploadFile(DVClientHelper dv, long updateObjectId, long parentId, long archiveId, String mimeType, String filePath, String fileName)  throws IOException, DException {
        System.out.println("uploadFile");
        // upload a new file
        int useType = Constants.OBJECT_USE_TYPE_USER;//1
        int pageNum = 1;
    
        boolean readSomeBytes = false;
        for (int i = 0; i < 5; i++) {
            FileInputStream fis = new FileInputStream(filePath);
            try {
                byte[] bytes = new byte[1];
                int numread = fis.read(bytes);
                if (numread > 0) {
                    readSomeBytes = true;
                    break;
                } else {
                    try {
						Thread.sleep(500);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
                }
            } finally {
                fis.close();
            }
        }

        if (!readSomeBytes) {
            throw new RuntimeException("Unable to read from file. Is it locked by another app?");
        }
       
        InputStream fileInputStream = new FileInputStream(filePath);
        
        try {
        	
        	long objectId = dv.getEngine().fileUpload(updateObjectId, parentId, archiveId, useType, pageNum, mimeType, fileName, fileInputStream);//12659294
            System.out.println("ObjectID after fileUpload(): " + objectId);
            return objectId;
        } finally {
            fileInputStream.close();
        }
    }
	private void showResult() {
		// TODO Auto-generated method stub
		AsyncTask<String, String, String> execute = new AsyncTask<String, String, String>() {
            public Exception exception;

            @Override
            protected String doInBackground(String... params) {
                
                try {

                	 dv = new DVClientHelper(); 
                     dv.setHessianHttpUrl(appUrl + "dvapi/2/h/");
                    dv.setConnectTimeout(10000);
                    
                    dv.connect(n, p);
                    dv.getEngine().touchSession();
                    String webSessionId = new String(dv.getSessionId().getKey());
                    DArchive[] archives = dv.getEngine().listArchives(Constants.ACTION_ARCHIVE_CREATERECORD);
                    System.out.println("Can upload to these archives:");
                    for (DArchive archive : archives) {
                        if (archive.isAllowObjects()) {
                            System.out.println(archive.getName());
                        }
                    }
                    
                    DVWebApiClient web = new DVWebApiClient(appUrl + "dvwebapi/h", webSessionId, true, 60000);
                    web.getWebApiProxy().setupWebSession(null, true);

                    String link = EmbedUtil.makeAppMainUrl(appUrl, webSessionId);

                    System.out.println("App home page: " + link);
                	sharedpreferences.edit().putString(UserNameKey, n).commit();
                	sharedpreferences.edit().putString(PasswordKey, p).commit();
                    return link;
                    
                } catch (Exception ex) {
                    this.exception = ex;
                    ex.printStackTrace();
					System.out.println(ex.getMessage());
                } 

                return  null;
            }
            
            @Override
            protected void onPostExecute(String link) {
                if (this.exception != null) {
                	sharedpreferences.edit().putBoolean(FLAT, false).commit();
                	showLogin();
                	if(flag == true) {
                		flag = false;
                	} else {
                		if(isConnectingToInternet()){
                			Toast.makeText(getApplicationContext(), this.exception.getMessage(), Toast.LENGTH_SHORT).show();
                		} else {
                			Toast.makeText(getApplicationContext(), "Internet Connection Fail!", Toast.LENGTH_SHORT).show();
                		}
                		
                	}
                }
                else {
                	Log.e("server address", link);
                	sharedpreferences.edit().putBoolean(FLAT, true).commit();
                	                	
                	Intent i1 = new Intent(DocuVantageLogin.this, DocuVantageEdit.class);
                	i1.putExtra("serverURL", link);
                	i1.putExtra("username", n);
                	i1.putExtra("password", p);
                	String webSessionId = new String(dv.getSessionId().getKey());
                	if(webSessionId == null) {
                		DocuVantageLogin.this.finish();
                	}
                	if(intentFlag) {
                		try {
							shareToApp(appUrl, webSessionId, dv, receivedUri);
						} catch (Exception e) {
							e.printStackTrace();
							System.out.println(e.getMessage());
						}
                		intentFlag = false;
                		return;
                	}
                	startActivity(i1);
                	DocuVantageLogin.this.finish();
                	
                	Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND_MULTIPLE); 
                    sharingIntent.setType("text/plain");
                    sharingIntent.setType("image/jpeg");
                    File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath() + File.separator + "sue.docx");
                    Uri uri = Uri.fromFile(file);
                    
                    sharingIntent.putExtra(Intent.EXTRA_STREAM, uri);
                    String shareBody = "Here is the share content body";
                    sharingIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "DocuVantage OnDemand");
                    sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, shareBody);
                    

                }
            }
        };
        execute.execute("executing...");
	}

	public boolean isConnectingToInternet(){
        ConnectivityManager connectivity = (ConnectivityManager)this.getSystemService(Context.CONNECTIVITY_SERVICE);
          if (connectivity != null) 
          {
              NetworkInfo[] info = connectivity.getAllNetworkInfo();
              if (info != null) 
                  for (int i = 0; i < info.length; i++) 
                      if (info[i].getState() == NetworkInfo.State.CONNECTED)
                      {
                          return true;
                      }
 
          }
          return false;
    }
	public String getFileNameFromPath(String input) {
		String[] temp;
		String delimeter = "/";
		temp = input.split(delimeter);
		return temp[temp.length - 1];
		}
	public void showLogin() {

			// Create Object of Dialog class
			final Dialog login = new Dialog(this);
			// Set GUI of login screen
			login.setContentView(R.layout.login_dialog);
			login.setTitle("Login to DocuVantage OnDemand");
			login.setCanceledOnTouchOutside(false);
			login.getWindow().setBackgroundDrawableResource(R.drawable.dialog_box);
			// Init button of login GUI
			btnLogin = (Button) login.findViewById(R.id.btnLogin);
			btnCancel = (Button) login.findViewById(R.id.btnCancel);
			txtUsername = (EditText)login.findViewById(R.id.txtUsername);
			txtPassword = (EditText)login.findViewById(R.id.txtPassword);
			
			rememberMe = (CheckBox)login.findViewById(R.id.rememberMe);
			
			name = (TextView)login.findViewById(R.id.txtUsername);
			pass = (TextView)login.findViewById(R.id.txtPassword);
			
			sharedpreferences = getSharedPreferences(MyPREFERENCES, Context.MODE_PRIVATE);
			n = txtUsername.getText().toString();
			p = txtPassword.getText().toString();
			login.show();

			if (sharedpreferences.contains(UserNameKey))
		      {
				
				name.setText(sharedpreferences.getString(UserNameKey, ""));

		      }
	        if (sharedpreferences.contains(PasswordKey))
	          {
	            pass.setText(sharedpreferences.getString(PasswordKey, ""));

	          }
	        if (sharedpreferences.contains(ChkRememberKey))
	          {
	        	
	        	rememberMe.setChecked(sharedpreferences.getBoolean(ChkRememberKey, false));
	            
	          }
		

			rememberMe.setOnClickListener(new CheckBox.OnClickListener(){
	    		public void onClick (View v){
	    			
	    			RememberMe();
	    		}
	    	});
			// Attached listener for login GUI button
			btnLogin.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					
					if(txtUsername.getText().toString().trim().length() > 0 && txtPassword.getText().toString().trim().length() > 0)
					{
						n = txtUsername.getText().toString().trim();
						p = txtPassword.getText().toString().trim();
						showResult();
						login.dismiss();
					}
					else
					{
						Toast.makeText(DocuVantageLogin.this, "Please enter Username and Password", Toast.LENGTH_LONG).show();
						return;

					}
				}
			});
			btnCancel.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					login.dismiss();
					DocuVantageLogin.this.finish();
				}
			});
	
	}

	private void RememberMe() {
		chk = rememberMe.isChecked();
		if(chk){
			n = name.getText().toString().trim();
			p = pass.getText().toString().trim();
			Editor editor = sharedpreferences.edit();
			editor.putString(UserNameKey, n);
			editor.putString(PasswordKey, p);
			editor.putBoolean(ChkRememberKey, chk);
			editor.commit();
		} else {
			n = "";
			p = "";
			Editor editor = sharedpreferences.edit();
			editor.putString(UserNameKey, n);
			editor.putString(PasswordKey, p);
			editor.putBoolean(ChkRememberKey, chk);
			editor.commit();
		}

	}
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
        	DocuVantageLogin.this.finish();
            return true;
        }

        return super.onKeyDown(keyCode, event);
    }
	@Override
	public void onClick(View arg0) {
		
	}
	public String getRealPathFromURI(Context context, Uri contentUri) {
		  Cursor cursor = null;
		  try { 
		    String[] proj = { MediaStore.Images.Media.DATA };
		    cursor = context.getContentResolver().query(contentUri,  proj, null, null, null);
		    int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
		    cursor.moveToFirst();
		    return cursor.getString(column_index);
		  } finally {
		    if (cursor != null) {
		      cursor.close();
		    }
		  }
	}
	public boolean isNumeric(String str)  
	{  
	  try  
	  {  
	    long d = Long.parseLong(str);  
	  }  
	  catch(NumberFormatException nfe)  
	  {  
		  nfe.printStackTrace();
			System.out.println(nfe.getMessage());
	    return false;  
	  }  
	  return true;  
	}
	public  String getPath(Uri uri) {
		String[] projection = { MediaStore.Images.Media.DATA };
		Cursor cursor = managedQuery(uri, projection, null, null, null);
		startManagingCursor(cursor);
		int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
		cursor.moveToFirst();
		return cursor.getString(column_index);
		}
	
}