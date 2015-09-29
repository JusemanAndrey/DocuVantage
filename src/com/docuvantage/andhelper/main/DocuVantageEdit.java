package com.docuvantage.andhelper.main;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.RemoteException;
import android.os.StrictMode;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.webkit.MimeTypeMap;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import com.docuvantage.andhelper.hessian.DVClientHelper;
import com.docuvantage.andhelper.util.EmbedUtil;
import com.dv.edm.api.bean.DArchive;
import com.dv.edm.api.bean.DFileRev;
import com.dv.edm.api.bean.DObject;
import com.dv.edm.api.bean.DUserSetting;
import com.dv.edm.api.constants.Constants;
import com.dv.edm.api.exception.DException;
import com.ipaulpro.afilechooser.utils.FileUtils;

public class DocuVantageEdit extends Activity implements OnTouchListener{
	public SharedPreferences sharedpreferences;
	public static final String MyPREFERENCES = DocuVantageLogin.MyPREFERENCES;
	public static final String UserNameKey = DocuVantageLogin.UserNameKey;
    public static final String PasswordKey = DocuVantageLogin.PasswordKey;
    public static final String ChkRememberKey = DocuVantageLogin.ChkRememberKey;
    public String TAG = this.getClass().getSimpleName();;
    public String name, pass, chkKey;
    public WebView newWebView;
    public String url;
	public String userAgent, ua;
	public String appUrl = "https://ltp3.docuvantage.com/";
	long myArchiveId = 999;
	public ProgressDialog progressDialog;
	public static final int progress_bar_type = 0;

	public boolean flag = false;
	public boolean editflag = false;
	final Context context = this;
	public Uri receivedUri;
	public long parentId = 0; // does not matter when updating existing file
	public long archiveId = 0; // does not matter when updating existing file
	public String mimeType = "";
	public boolean installedPackageFlag = false;
    public long obj_id;
    public DVClientHelper dd = new DVClientHelper();
    public String webSessionId = new String(DocuVantageLogin.dv.getSessionId().getKey());
    public File downloadfile;
    public File file;
    public long objectIdUpLoadFile;
    public int revision = 0;
    boolean shareFlag = false;
    public int index = 0;
    
	@Override
    public void onCreate(Bundle savedInstanceState){
    	super.onCreate(savedInstanceState);
    	setContentView(R.layout.activity_main);
    	
    	Intent intent = getIntent();
    	url= intent.getStringExtra("serverURL");
    	name = intent.getStringExtra("username");
    	pass = intent.getStringExtra("password");
    	
    	userAgent = "ANDROID;"+ System.getProperty("os.version") + ";" + //os version
    						android.os.Build.VERSION.INCREMENTAL + ";" + 
    						android.os.Build.VERSION.SDK_INT + ";" + //api version
    						android.os.Build.DEVICE + ";" + //android device
    						android.os.Build.MODEL + ";" + //model 
    						android.os.Build.PRODUCT;//product
    	
    	newWebView = (WebView)findViewById(R.id.newWebView);
    	startWebView(url);
    	
	}
	public void shareToApp(final String appUrl, final String webSessionId, final DVClientHelper dv, final Uri theUri) throws Exception {
        
        shareFlag = true;
        
        if (android.os.Build.VERSION.SDK_INT > 9) {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }
        final DArchive[] archives = dv.getEngine().listArchives(Constants.ACTION_ARCHIVE_CREATERECORD);
        final String[] archive111 = new String[archives.length];
 
        for (DArchive archive : archives) {
            if (archive.isAllowObjects()) {
            	String a = archive.getName().toString();            	
            	archive111[index] = a;
            	index++;
            }
        }
       
        final File file = new File(theUri.getPath());
        final String filePath = file.toString();
        int startIndex = filePath.lastIndexOf("/");
        final String fileName = filePath.substring(startIndex+1);
  
      
        AlertDialog.Builder builder = new AlertDialog.Builder(this);  
	     builder.setTitle("Upload To Archive");  
	     
	     builder.setItems(archive111, new DialogInterface.OnClickListener() {  
	       @Override  
	       public void onClick(DialogInterface dialog, int which) {  
	    	   String link = null;
	    	   // it will always be one file
	           long archiveId = archives[which].getId(); // this is just picking the first archive, it should use the one the user selected
	           String mimeType = getMimeType(filePath);
	           
		    	try {
		    		File new_upload_file = new File(filePath);
		    		long new_upload_file_size = new_upload_file.length();
		    		
		    		if(new_upload_file_size == 0){
		    			Toast.makeText(getApplicationContext(), "Cannot upload empty file", Toast.LENGTH_LONG).show();
		    		} else {
		    			
		    			long newobjectId = uploadFile(dv, 0, 0, archiveId, mimeType, filePath, fileName);
		    			
						try {
							
							link = EmbedUtil.makeCreateRecordUrl(appUrl, webSessionId, archiveId, newobjectId, fileName, Constants.OBJECT_TYPE_FILE);
							newWebView = (WebView)findViewById(R.id.newWebView);
							startWebView(link);
							
						} catch (Exception e) {
							System.out.println(e.getMessage());
							e.printStackTrace();
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
	             DocuVantageEdit.this.finish();
	           }  
	         });  
	     AlertDialog alert = builder.create();  
	     alert.show();  
    }
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
	    switch (requestCode) {
	        case 100:   
	            if (resultCode == RESULT_OK) {

	                final Uri uri = data.getData();

	                // Get the File path from the Uri
	                String path = FileUtils.getPath(this, uri);
	                // Alternatively, use FileUtils.getFile(Context, Uri)
	                if (path != null && FileUtils.isLocal(path)) {
	                    File file = new File(path);
	                    long file_size_recovery = file.length();
	                    try {
	                    	if(file_size_recovery == 0){
	                    		Toast.makeText(getApplicationContext(), "Cannot upload empty file", Toast.LENGTH_LONG).show();
	                    	} else {
	                    		uploadFile(DocuVantageLogin.dv, objectIdUpLoadFile, 0, 0, mimeType, path, file.getName());
	                    	}
						} catch (DException e) {
							e.printStackTrace();
						} catch (IOException e) {
							e.printStackTrace();
						}
	                }
	            }
	            break;
	    }
	}
	File newfile = null;
	@Override
	protected void onResume() {
		super.onResume();
		if(flag && editflag){
			flag = false;
			editflag = false;
			
			AlertDialog.Builder builder = new AlertDialog.Builder(context);
			builder.setTitle("File Save");
	        builder.setMessage("Do you want to save your changes to the server?")
	               .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
	                   public void onClick(DialogInterface dialog, int id) {
	                	try {
	                	    dialog.dismiss();
	                	    DFileRev dObjectFileRev = (DFileRev) DocuVantageLogin.dv.getEngine().getFileInfoForRev(obj_id, revision);
	    		            newfile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath() + File.separator + dObjectFileRev.getName());
	    		            mimeType = getMimeType(newfile.toString());
	    		            long obj_id1 = obj_id;
	    		            
	    		            long edit_after_upload_file_size = newfile.length();
	    		            
	    		            if(edit_after_upload_file_size == 0){
	    		            	Toast.makeText(getApplicationContext(), "Cannot upload empty file", Toast.LENGTH_LONG).show();
	    		            } else {
	    		            	long a = uploadFile(DocuVantageLogin.dv, obj_id1, 0, 0, mimeType, newfile.toString(), dObjectFileRev.getName());
	    		            }
	    		            
							try {
					            // for some reason this is throwing an exception, just ignore it for now
								DocuVantageLogin.dv.getEngine().checkin(obj_id1);
					        } catch (Exception ex) {
					            Log.e("Exception checking in file", ex.toString());
					            ex.printStackTrace();
								System.out.println(ex.getMessage());
					            Toast.makeText(getApplicationContext(), ""+ex.getMessage().toString(), Toast.LENGTH_LONG).show();
					        }
						} catch (DException e) {
							e.printStackTrace();
						} catch (IOException e) {
							e.printStackTrace();
						}
	                   }
	               })
	               .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
	                   public void onClick(DialogInterface dialog, int id) {
	                	   if(newfile.isFile()){
	                		   newfile.delete();
	                	   }
	                       dialog.dismiss();
	                   }
	               });
	        builder.create().show();
		}

	}
	
	public void startWebView(final String _url) {

		newWebView.setWebViewClient(new WebViewClient() {      
	        ProgressDialog progressDialog;
	        
			public boolean shouldOverrideUrlLoading(WebView view, String _url) {
	        	try {
		        	String webSessionId = new String(DocuVantageLogin.dv.getSessionId().getKey());
		        	if(webSessionId == null){
		        		Intent i = new Intent(DocuVantageEdit.this, DocuVantageLogin.class);
		        		startActivity(i);
		        	}
		        	if(_url.startsWith("docuvantageondemand://logout")){
		        			logout();
		        	}
		        	if(_url.startsWith("docuvantageondemand://open")){
		        		int first_slash = _url.indexOf("/");
		        		int second_slash = first_slash+1;
		        		int third_slash = _url.substring(second_slash+1).indexOf("/")+second_slash+1;
		        		int forth_slash = _url.substring(third_slash+1).indexOf("/")+third_slash+1;
		        		int five_slash = _url.substring(forth_slash+1).indexOf("/")+forth_slash+1;
		        		
		        		String editFlag_val = _url.substring(five_slash+1);
		        		long objectid = Long.parseLong(_url.substring(third_slash+1, forth_slash));
		        		if(editFlag_val.equals("false")) {//open work	        			
							openFile(DocuVantageLogin.dv, appUrl, webSessionId, objectid);						
		        		}
		        		if(editFlag_val.equals("true")){//edit work
		        			Log.e("edit work", "true");	        			
							editFile(DocuVantageLogin.dv, appUrl, webSessionId, objectid);						
		        		}		        		
		        		
		        		_url = url;
		        		return false;
		        	}
		        	if(_url.startsWith("docuvantageondemand://checkout")){
		        		try {		        			
							checkoutFile(DocuVantageLogin.dv, appUrl, webSessionId, _url);
						} catch (Exception e) {
							e.printStackTrace();
						}
		        		return true;
		        	}
		        	if(_url.startsWith("docuvantageondemand://checkin")){
		        		try {
							checkinFile(DocuVantageLogin.dv, appUrl, webSessionId, _url);
						} catch (Exception e) {
							e.printStackTrace();
						}
		        		return true;
		        	}
		        	
		            view.loadUrl(_url);
	            
	        	} catch (DException e) {
	        		e.printStackTrace();
				} catch (MalformedURLException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
	        	return true;
	        }
	    
	        //Show loader on url load
	        public void onLoadResource (WebView view, String url) {
	            if (progressDialog == null) {
	                progressDialog = new ProgressDialog(DocuVantageEdit.this);
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
		
	     // Javascript inabled on webview  
		newWebView.getSettings().setJavaScriptEnabled(true); 	    
		newWebView.getSettings().setLoadWithOverviewMode(true);
		newWebView.getSettings().setUseWideViewPort(true);
	    newWebView.setScrollBarStyle(WebView.SCROLLBARS_OUTSIDE_OVERLAY);
	    newWebView.setScrollbarFadingEnabled(false);
	    newWebView.getSettings().setBuiltInZoomControls(true);
	    newWebView.getSettings().setDisplayZoomControls(false);
	    newWebView.clearHistory();
	    newWebView.clearFormData();
	    newWebView.clearCache(true);
	    
	    newWebView.getSettings().setUserAgentString(userAgent);
	    //Load url in webview
	    newWebView.loadUrl(url);
	    newWebView.setOnTouchListener(new View.OnTouchListener() {

            public boolean onTouch(View v, MotionEvent event) { 
            	
	                WebView.HitTestResult hr = ((WebView)v).getHitTestResult();
            	
            	return false;
            }

     		
        });
	    
	}
	
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case progress_bar_type:
			progressDialog = new ProgressDialog(this);
			progressDialog.setMessage("Downloading file. Please wait...");
			progressDialog.setMax(100);
			progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
			progressDialog.show();
			return progressDialog;
		default:
			return null;
		}
	}

	public void downloadFile(final DVClientHelper dv, final String appUrl, final String webSessionId, final long objectId) throws IOException, DException, MalformedURLException {
		AsyncTask<String, String, String> execute = new AsyncTask<String, String, String>() {			
            public Exception exception;
            @Override
    		protected void onPreExecute() {
    			super.onPreExecute();
    			progressDialog = ProgressDialog.show(DocuVantageEdit.this, "Please Wait", "File Downloading...");
    		}
            
            @Override
            protected String doInBackground(String... params) {
		        try {
		        	
		        	String webSessionId = new String(dv.getSessionId().getKey());
		        	int revision1 = 0;
		            if (revision1 == 0) {
		                // if revision is 0 then make a call to get the current revision
		                DObject objectInfo = DocuVantageLogin.dv.getEngine().getObjectInfo(objectId);
		                revision = objectInfo.getCurRevision();
		            }

		            DFileRev dObjectFileRev = (DFileRev)dv.getEngine().getFileInfoForRev(objectId, revision);

		            file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath() + File.separator + dObjectFileRev.getName());
		            long file_size = Long.parseLong(String.valueOf(file.length()));
		            

		            System.out.println("Downloading file to path = " + file.getPath());
		            System.out.println("File size = " + dObjectFileRev.getFileSize());

		            String downloadFileServlet = appUrl + "DownloadFileServlet;jsessionid=" + webSessionId;
		            String realUrl = downloadFileServlet + "?objectId=" + objectId + "&revision=" + revision;
		            URL url = new URL(downloadFileServlet + "?objectId=" + objectId + "&revision=" + revision);
		            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		            
		            URLConnection conection = url.openConnection();
		            conection.connect();
		            // getting file length
		            int lenghtOfFile = conection.getContentLength();
		            
		            try {
		                InputStream is = conn.getInputStream();
		                try {
		                	int numBytesRead = 0;
		                    int BUFSIZE = 1024 * 16; // 16KB
		                    byte[] bytes = new byte[BUFSIZE];
		                    FileOutputStream fos = new FileOutputStream(file);
		                    try {
		                    	publishProgress(""+numBytesRead);
		                        while (true) {
		                            int nread = is.read(bytes);
		                            numBytesRead += nread;
		                            if (nread <= -1) {
		                                break;
		                            } else {
		                            	if(numBytesRead > 1024 * 1024){
		                            		numBytesRead = 0;
		                            		fos.flush();
		                            	}
		                            	publishProgress(""+(int)((numBytesRead*100)/lenghtOfFile));
		                                fos.write(bytes, 0, nread);
		                            }
		                            
		                        }
		                        numBytesRead = 0;
		                        
		                    } finally {
		                        try {
		                            fos.close();
		                        } catch (Exception e) {
		                        	e.printStackTrace();
		        					System.out.println(e.getMessage());
		                        }
		                    }

		                } finally {
		                    try {
		                        is.close();
		                    } catch (Exception e) {
		                    	e.printStackTrace();
		    					System.out.println(e.getMessage());
		                    }
		                }
		            } catch (IOException ex) {
		            	ex.printStackTrace();
						System.out.println(ex.getMessage());
		                // try to extract the error message from the response
		                if (conn.getResponseCode() == 500) {
		                    byte[] bytes = new byte[4096];
		                    int numBytes = conn.getErrorStream().read(bytes);
		                    String errorMsg = new String(bytes, 0, numBytes);
		                    // try to extract the text in the h1 tag
		                    int pos1 = errorMsg.indexOf("<h1>");
		                    if (pos1 >= 0) {
		                        errorMsg = errorMsg.substring(pos1 + 4);
		                        int pos2 = errorMsg.indexOf("</h1>");
		                        if (pos2 >= 0) {
		                            errorMsg = errorMsg.substring(0, pos2);
		                        }
		                    }
		                    // keep the stack trace from the original exception
		                    final IOException ex2 = new IOException(errorMsg);
		                    ex2.setStackTrace(ex.getStackTrace());
		                    throw ex2;
		                }
		            }

//		           return realUrl;
		            obj_id = objectId;
                    String fileSize = String.valueOf(file_size);
                    String fileString = file.toString();
                    
                    return fileString;

 		        } catch (Exception ex) {
		        	this.exception = ex;
		        	ex.printStackTrace();
					System.out.println(ex.getMessage());
		        }
		        return file.toString();
            }
   
            protected void onProgressUpdate(String... progress) {
    			// setting progress percentage
            	progressDialog.setProgress(Integer.parseInt(progress[0]));
            	progressDialog.setCanceledOnTouchOutside(false);
           }
            
            @Override
            protected void onPostExecute(String fileStr) {
            	progressDialog.dismiss();
            	if (this.exception != null) {  
            		
                	Toast.makeText(getApplicationContext(), "File download Fail         "+this.exception.getMessage(), Toast.LENGTH_LONG).show();
                } else {
                		boolean isInstalled = packageInfo();
	            		if(isConnectingToInternet()){
		            		File file = new File(fileStr);
		            		Uri uri = Uri.fromFile(file);
		                    Intent intent = new Intent(Intent.ACTION_VIEW);
		//		            Intent intent = new Intent(Intent.ACTION_MAIN, null);
		                    if (file.toString().contains(".doc") || file.toString().contains(".docx") || file.toString().contains(".DOC") || file.toString().contains(".DOCX") 
		                    		|| file.toString().contains(".Doc") || file.toString().contains(".Docx")) {
		                        // Word document
		                        intent.setDataAndType(uri, "application/msword");
		                    } else if(file.toString().contains(".xls") || file.toString().contains(".xlsx")) {
		                        // Excel file
		                        intent.setDataAndType(uri, "application/vnd.ms-excel");
		                    } else if(file.toString().contains(".pdf")) {
		                        // PDF file
		                        intent.setDataAndType(uri, "application/pdf");
		                    } 
		                    
		                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK|Intent.FLAG_ACTIVITY_NEW_TASK); 
		                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		                    
		                    try{
		                        startActivity(intent);
		                        flag = true;
		                        obj_id = objectId;
		                    } catch(Exception e){
		                    	e.getMessage();
		                    	e.printStackTrace();
		    					System.out.println(e.getMessage());
	//	                    	if(!isInstalled){
		                    		Toast.makeText(getApplicationContext(), "You do not have an app that can open this file type.\r\nFor Office files try Polaris Office or OfficeSuite", Toast.LENGTH_LONG).show();
	//	                    	}
		                    }
	            		} else {
	            			Toast.makeText(getApplicationContext(), "Internet Connection Failed", Toast.LENGTH_LONG).show();
	            		}
            	}
            }
        };
        execute.execute("");

	}
	public File downloadFile1(final DVClientHelper dv, final String appUrl, final String webSessionId, final long objectId1) throws IOException, DException, MalformedURLException {
		
		AsyncTask<String, String, String> execute = new AsyncTask<String, String, String>() {			
            public Exception exception;
            @Override
    		protected void onPreExecute() {
    			super.onPreExecute();
    			progressDialog = ProgressDialog.show(DocuVantageEdit.this, "Please Wait", "File Downloading...");
    		}
            
            @Override
            protected String doInBackground(String... params) {
            		        	
		        	String webSessionId = new String(dv.getSessionId().getKey());
		        	String fileString = "";
		        	int revision1 = 0;
		            if (revision1 == 0) {
		                DObject objectInfo = DocuVantageLogin.dv.getEngine().getObjectInfo(objectId1);
		                revision = objectInfo.getCurRevision();
		            }

		            DFileRev dObjectFileRev = (DFileRev)dv.getEngine().getFileInfoForRev(objectId1, revision);

		            downloadfile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath() + File.separator + dObjectFileRev.getName());
		            long file_size = Long.parseLong(String.valueOf(downloadfile.length()));
		            

		            System.out.println("Downloading file to path = " + downloadfile.getPath());
		            System.out.println("File size = " + dObjectFileRev.getFileSize());
		            
		            try {
		            	
		            String downloadFileServlet = appUrl + "DownloadFileServlet;jsessionid=" + webSessionId;
		            String realUrl = downloadFileServlet + "?objectId=" + objectId1 + "&revision=" + revision;
		            URL url = new URL(downloadFileServlet + "?objectId=" + objectId1 + "&revision=" + revision);
		            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		            
		            URLConnection conection = url.openConnection();
		            conection.connect();
		            // getting file length
		            int lenghtOfFile = conection.getContentLength();
		            
		            try {
		                InputStream is = conn.getInputStream();
		                try {
		                	int numBytesRead = 0;
		                    int BUFSIZE = 1024 * 16; // 16KB
		                    byte[] bytes = new byte[BUFSIZE];
		                    FileOutputStream fos = new FileOutputStream(downloadfile);
		                    try {
		                    	publishProgress(""+numBytesRead);
		                        while (true) {
		                            int nread = is.read(bytes);
		                            numBytesRead += nread;
		                            if (nread <= -1) {
		                                break;
		                            } else {
		                            	if(numBytesRead > 1024 * 1024){
		                            		numBytesRead = 0;
		                            		fos.flush();
		                            	}
		                            	publishProgress(""+(int)((numBytesRead*100)/lenghtOfFile));
		                                fos.write(bytes, 0, nread);
		                                // TODO: update progress using numBytesRead
		                            }
		                            
		                        }
		                        numBytesRead = 0;
		                        
		                    } finally {
		                        try {
		                            fos.close();
		                        } catch (Exception ex) {
		                        	ex.printStackTrace();
		        					System.out.println(ex.getMessage());
		                        }
		                    }

		                } finally {
		                    try {
		                        is.close();
		                    } catch (Exception ex) {
		                    	ex.printStackTrace();
		    					System.out.println(ex.getMessage());
		                    }
		                }
		            } catch (IOException ex) {
		            	ex.printStackTrace();
						System.out.println(ex.getMessage());
		                // try to extract the error message from the response
		                if (conn.getResponseCode() == 500) {
		                    byte[] bytes = new byte[4096];
		                    int numBytes = conn.getErrorStream().read(bytes);
		                    String errorMsg = new String(bytes, 0, numBytes);
		                    // try to extract the text in the h1 tag
		                    int pos1 = errorMsg.indexOf("<h1>");
		                    if (pos1 >= 0) {
		                        errorMsg = errorMsg.substring(pos1 + 4);
		                        int pos2 = errorMsg.indexOf("</h1>");
		                        if (pos2 >= 0) {
		                            errorMsg = errorMsg.substring(0, pos2);
		                        }
		                    }
		                    // keep the stack trace from the original exception
		                    final IOException ex2 = new IOException(errorMsg);
		                    ex2.setStackTrace(ex.getStackTrace());
		                    throw ex2;
		                }
		            }

                    String fileSize = String.valueOf(file_size);
                    fileString = downloadfile.toString();

 		        } catch (Exception ex) {
		        	this.exception = ex;

		        	ex.printStackTrace();
					System.out.println(ex.getMessage());
		        }
		        
				return fileString;
            }
  
            protected void onProgressUpdate(String... progress) {
    			// setting progress percentage
            	progressDialog.setProgress(Integer.parseInt(progress[0]));
            	progressDialog.setCanceledOnTouchOutside(false);
           }
            
            @Override
            protected void onPostExecute(String fileStr) {
            	progressDialog.dismiss();
            	if (this.exception != null) {            		
                	Toast.makeText(getApplicationContext(), "File download Fail          "+this.exception.getMessage(), Toast.LENGTH_LONG).show();
                } else {
                		boolean isInstalled = packageInfo();
//	            	
	            		File file = new File(fileStr);
	            		Uri uri = Uri.fromFile(file);
	                    Intent intent = new Intent(Intent.ACTION_VIEW);
	//		            Intent intent = new Intent(Intent.ACTION_MAIN, null);
	                    if (file.toString().contains(".doc") || file.toString().contains(".docx")) {
	                        // Word document
	                        intent.setDataAndType(uri, "application/msword");
	                    } else if(file.toString().contains(".xls") || file.toString().contains(".xlsx")) {
	                        // Excel file
	                        intent.setDataAndType(uri, "application/vnd.ms-excel");
	                    } else if(file.toString().contains(".pdf")) {
	                        // PDF file
	                        intent.setDataAndType(uri, "application/pdf");
	                    } else {
	                    	Toast.makeText(getApplicationContext(), "valid file format", Toast.LENGTH_LONG).show();
	                    }
	                    
	                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK|Intent.FLAG_ACTIVITY_NEW_TASK); 
	                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
	
//	                    if (intent.resolveActivity(getPackageManager()) != null) {
	                    try{
	                        startActivity(intent);
	                        flag = true;
	                        obj_id = objectId1;
	                    } catch(Exception e){
	                    	e.printStackTrace();
	    					System.out.println(e.getMessage());

	                    		Toast.makeText(getApplicationContext(), "You do not have an app that can open this file type.\r\nFor Office files try Polaris Office or OfficeSuite", Toast.LENGTH_LONG).show();

	                    }
            	}
            }
        };
        execute.execute("");
        Log.e("file", downloadfile.toString());
		return downloadfile;
		
	}
	private boolean packageInfo(){
		final Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
		  mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
		  final List pkgAppsList = getPackageManager().queryIntentActivities( mainIntent, 0);
		  
		  for (Object object : pkgAppsList) 
		  {
			    ResolveInfo info = (ResolveInfo) object;
			    Drawable icon    = getBaseContext().getPackageManager().getApplicationIcon(info.activityInfo.applicationInfo);
			    String strAppName  	= info.activityInfo.applicationInfo.publicSourceDir.toString();
			    String strPackageName  = info.activityInfo.applicationInfo.packageName.toString();
			    final String title 	= (String)((info != null) ? getBaseContext().getPackageManager().getApplicationLabel(info.activityInfo.applicationInfo) : "???");
			    
			    
			    if(strAppName.startsWith("/data/app/com.infraware.office.link")){
			    	installedPackageFlag = true;
			    } 
			    if(strAppName.startsWith("/data/app/com.mobisystems.office")){
			    	installedPackageFlag = true;
			    }
			    if(strAppName.startsWith("/data/app/com.google.android.apps.docs.editors.docs")){
			    	installedPackageFlag = true;
			    }
			    
	     }

		return installedPackageFlag;
	}
	public void logout() {
    		
			try {
				sharedpreferences = getSharedPreferences(MyPREFERENCES, 0);
            	Editor editor = sharedpreferences.edit();
				boolean chkKey = sharedpreferences.getBoolean(ChkRememberKey, false); 
				if(!chkKey){
	            	editor.remove(UserNameKey);
	            	editor.remove(PasswordKey);
	            	editor.clear();
	            	editor.commit();
				}
				webSessionId = "";
				dd.disconnect();
				DocuVantageEdit.this.finish();
			} catch (RemoteException e) {
				e.printStackTrace();
			}
	}

	@Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if ((keyCode == KeyEvent.KEYCODE_BACK) && this.newWebView.canGoBack()) {
            this.newWebView.goBack();
        	try {
				showHomePage(appUrl, webSessionId);
			} catch (Exception e) {
				e.printStackTrace();
			}
            return true;
        }

        return super.onKeyDown(keyCode, event);
    }
	@Override  
    public boolean onCreateOptionsMenu(Menu menu) {  
        // Inflate the menu; this adds items to the action bar if it is present.  
	    getMenuInflater().inflate(R.menu.main_menu, menu);
	    return true;  
    }  
		
    @Override  
    public boolean onOptionsItemSelected(MenuItem item) {  
        switch (item.getItemId()) {  
            case R.id.item1:  
            	sharedpreferences = getSharedPreferences(MyPREFERENCES, 0);
            	Editor editor = sharedpreferences.edit();
            	editor.remove(UserNameKey);
            	editor.remove(PasswordKey);
            	editor.clear();
            	editor.commit();
				
                Toast.makeText(getApplicationContext(),"Deleted remembered login and password",Toast.LENGTH_LONG).show();  
                return true;  

            default:  
                return super.onOptionsItemSelected(item);  
        }  
    }

	@Override
	public boolean onTouch(View arg0, MotionEvent arg1) {
		return false;
	}  
	public void openFile(DVClientHelper dv, String appUrl, String webSessionId, long objectId) throws DException, MalformedURLException, IOException {
        System.out.println("openFile");
        downloadFile(dv, appUrl, webSessionId, objectId);
    }

    public void editFile(DVClientHelper dv, String appUrl, String webSessionId, long objectId) throws DException, MalformedURLException, IOException {
        System.out.println("editFile");
        
        if (android.os.Build.VERSION.SDK_INT > 9) {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }
        int HOURS = 24;
        dv.getEngine().checkout(objectId, 0, HOURS);

        downloadFile(dv, appUrl, webSessionId, objectId);
        editflag = true;

    }

    public long uploadFile(final DVClientHelper dv, final long updateObjectId, final long parentId, final long archiveId, final String mimeType, final String filePath, final String fileName) throws IOException, DException {
        System.out.println("uploadFile");
        
        AsyncTask<String, String, String> upload = new AsyncTask<String, String, String>() {			
            public Exception exception;
            @Override
    		protected void onPreExecute() {
    			super.onPreExecute();
    			progressDialog = ProgressDialog.show(DocuVantageEdit.this, "Please Wait", "File Uploading...");
    		}            
            @Override
            protected String doInBackground(String... params) {
		           
		        try {
		        	int useType = Constants.OBJECT_USE_TYPE_USER;
			        int pageNum = 1;
			        
		        	InputStream fileInputStream = new FileInputStream(filePath);
		            objectIdUpLoadFile = dv.getEngine().fileUpload(updateObjectId, parentId, archiveId, useType, pageNum, mimeType, fileName, fileInputStream);
		            System.out.println("ObjectID after fileUpload(): " + objectIdUpLoadFile);
		            
		            fileInputStream.close();
		        } catch(Exception e) {
		        	e.printStackTrace();
					System.out.println(e.getMessage());
		        }
				return fileName;
            }
            protected void onProgressUpdate(String... progress) {
    			// setting progress percentage
            	progressDialog.setProgress(Integer.parseInt(progress[0]));
            	progressDialog.setCanceledOnTouchOutside(false);
           }
            
            @Override
            protected void onPostExecute(String fileStr) {
            	progressDialog.dismiss();
            	Toast.makeText(getApplicationContext(), "File uploaded successfully", Toast.LENGTH_LONG).show();
            }
        };
        upload.execute("");
        long b = objectIdUpLoadFile;
		return objectIdUpLoadFile;
    }
    public void cacheFileclear() {
        File[] directory = getCacheDir().listFiles();
        if(directory != null){
            for (File file : directory ){
                file.delete();
            }
         }
     }
    public void showHomePage(String appUrl, String webSessionId) throws Exception {
        System.out.println("showHomePage");
        // make the link we need for the home page
        if(webSessionId == null){
        	DocuVantageEdit.this.finish();
        }
        String link = EmbedUtil.makeAppMainUrl(appUrl, webSessionId);

        // show the link
        System.out.println("App home page: " + link);
    }
    public String getMimeType(String url) {
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
    private void checkoutFile(DVClientHelper dv, String appUrl, String webSessionId, String Url) throws Exception {
        System.out.println("checkoutFile");
        File chkfile = null;
        if (android.os.Build.VERSION.SDK_INT > 9) {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }
        
        int first_slash = Url.indexOf("/");
		int second_slash = first_slash+1;
		int third_slash = Url.substring(second_slash+1).indexOf("/")+second_slash+1;
		int forth_slash = Url.substring(third_slash+1).indexOf("/")+third_slash+1;
		int five_slash = Url.substring(forth_slash+1).indexOf("/")+forth_slash+1;
        int slash_index = Url.lastIndexOf("/");
        
        int mode = Integer.parseInt(Url.substring(forth_slash+1, five_slash));
        
		boolean openAfterDownload = Boolean.parseBoolean(Url.substring(five_slash+1, slash_index));
		int hours = Integer.parseInt(Url.substring(slash_index+1));

		long openObjectId = Long.parseLong(Url.substring(third_slash+1, forth_slash));
		long objectId = openObjectId;

        if (mode == 0) {
        	dv.getEngine().checkout(openObjectId, 0, hours);
        }
//        clearApplicationData();
        chkfile = downloadFile2(dv, appUrl, webSessionId, openObjectId);

        DUserSetting userSetting = new DUserSetting();
        final String key = "checkedout_path_" + openObjectId;
        userSetting.setName(key);
        userSetting.setValue(chkfile.getPath());
        userSetting.setType(Constants.USER_SETTING_TYPE_MISC);
        userSetting.setUserCanChange(true);
        DUserSetting[] userSettings = {userSetting};
        dv.getEngine().setUserSettings(userSettings);

        if (openAfterDownload) {
    		Uri uri = Uri.fromFile(chkfile);
            Intent intent = new Intent(Intent.ACTION_VIEW);
//		            Intent intent = new Intent(Intent.ACTION_MAIN, null);
            if (chkfile.toString().contains(".doc") || chkfile.toString().contains(".docx")) {
                // Word document
                intent.setDataAndType(uri, "application/msword");
            } else if(chkfile.toString().contains(".xls") || chkfile.toString().contains(".xlsx")) {
                // Excel file
                intent.setDataAndType(uri, "application/vnd.ms-excel");
            } else if(chkfile.toString().contains(".pdf")) {
                // PDF file
                intent.setDataAndType(uri, "application/pdf");
            } else {
            	Toast.makeText(getApplicationContext(), "valid file format", Toast.LENGTH_LONG).show();
            }
            
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK|Intent.FLAG_ACTIVITY_NEW_TASK); 
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        }
        showHomePage(appUrl, webSessionId);
       
    }


    private void checkinFile(DVClientHelper dv, String appUrl, String webSessionId, String Url) throws Exception {
        System.out.println("checkinFile");


        if (android.os.Build.VERSION.SDK_INT > 9) {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }
        
        int first_slash = Url.indexOf("/");
		int second_slash = first_slash+1;
		int third_slash = Url.substring(second_slash+1).indexOf("/")+second_slash+1;
		int forth_slash = Url.substring(third_slash+1).indexOf("/")+third_slash+1;
		int five_slash = Url.substring(forth_slash+1).indexOf("/")+forth_slash+1;
        
        String updateBeforeCheckinFlag = Url.substring(forth_slash+1);//false/true/false
        int start = updateBeforeCheckinFlag.indexOf("/");
        int end = updateBeforeCheckinFlag.lastIndexOf("/");
        
        long openObjectId = Long.parseLong(Url.substring(third_slash+1, forth_slash));
        boolean updateFile = Boolean.parseBoolean(updateBeforeCheckinFlag.substring(0, start));
        boolean deleteAfterCheckIn = Boolean.parseBoolean(updateBeforeCheckinFlag.substring(start+1, end));
        boolean promptForDifferentFile = Boolean.parseBoolean(updateBeforeCheckinFlag.substring(end+1));

        long objectId = openObjectId;

        long parentId = 0; // does not matter when updating existing file
        long archiveId = 0; // does not matter when updating existing file
        
        String filePath = null;
        if (promptForDifferentFile) {
            // prompt for the file
        	Intent getContentIntent = FileUtils.createGetContentIntent();
    	    Intent intentFile = Intent.createChooser(getContentIntent, "Select a file");
    	    startActivityForResult(intentFile, 100);
        }
        final DUserSetting[] userSettings = dv.getEngine().getUserSettingsByType(Constants.USER_SETTING_TYPE_MISC);
        if (userSettings.length == 0) {
            // prompt for the file
        	Intent getContentIntent = FileUtils.createGetContentIntent();
    	    Intent intentFile = Intent.createChooser(getContentIntent, "Select a file");
    	    startActivityForResult(intentFile, 100);
        } else {
            String key = "checkedout_path_" + objectId;
            for (DUserSetting us : userSettings) {
                if (us.getName().equals(key)) {
                    filePath = us.getValue();
                    Log.e("filePath", filePath);
                    break;
                }
            }
        }

        if (filePath == null) {
        	Toast.makeText(getApplicationContext(), "There is no checked out file", Toast.LENGTH_LONG).show();
            throw new RuntimeException("A file must be selected");
        }

        File checkinfile = new File(filePath);
        String mimeType = getMimeType(checkinfile.getPath());
        
        if(mimeType == null){
        	Toast.makeText(getApplicationContext(), "There is no checked out file", Toast.LENGTH_LONG).show();
        } else {
            if (updateFile && checkinfile.isFile()) {

            		uploadFile(dv, objectId, parentId, archiveId, mimeType, filePath, checkinfile.getName());
            }
            if(!checkinfile.isFile()) {
            	Toast.makeText(getApplicationContext(), "There is no checked out file in your phone", Toast.LENGTH_LONG).show();
            }
        }
        
        try {
        	
            dv.getEngine().checkin(objectId);
        } catch (Exception ex) {
        	ex.printStackTrace();
			System.out.println(ex.getMessage());
            Log.e("Exception checking in file", ""+ex.getMessage());
        }

        if (deleteAfterCheckIn) {
        	checkinfile.delete();

        }
        showHomePage(appUrl, webSessionId);
        
    }
    
    
    public File downloadFile2(DVClientHelper dv, String appUrl, String webSessionId, long objectid) throws IOException, DException, MalformedURLException {
        System.out.println("downloadFile");

        int revision1 = 0;
        if (revision1 == 0) {
            // if revision is 0 then make a call to get the current revision
            final DObject objectInfo = dv.getEngine().getObjectInfo(objectid);
            revision = objectInfo.getCurRevision();
        }
        
        DFileRev dObjectFileRev = (DFileRev) dv.getEngine().getFileInfoForRev(objectid, revision);

        
       File file2 = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath() + File.separator + dObjectFileRev.getName());
        if(file2.isFile()){
        	file2.delete();
        }
        
        System.out.println("Downloading file to path = " + file2.getPath());
        System.out.println("File size = " + dObjectFileRev.getFileSize());

        String downloadFileServlet = appUrl + "DownloadFileServlet;jsessionid=" + webSessionId;
        
        URL url = new URL(downloadFileServlet + "?objectId=" + objectid + "&revision=" + revision);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setDoInput(true);
        conn.setDoOutput(true);
        conn.setUseCaches(false);
        try {
        	
            InputStream is = conn.getInputStream();

            try {
            	
                int numBytesRead = 0;
                int BUFSIZE = 1024 * 16; // 16KB
                byte[] bytes = new byte[BUFSIZE];
                FileOutputStream fos = new FileOutputStream(file2);
                try {
                    while (true) {
                        int nread = is.read(bytes);
                        numBytesRead += nread;
                        if (nread <= -1) {
                            break;
                        } else {
                            fos.write(bytes, 0, nread);
                            // TODO: update progress using numBytesRead
                        }
                    }
                } finally {
                    try {
                    	fos.flush();
                        fos.close();
                        
                    } catch (Exception ex) {
                    	ex.printStackTrace();
    					System.out.println(ex.getMessage());
                    }
                }

            } finally {
                try {
                    is.close();
                    conn.disconnect();
                    
                    Toast.makeText(getApplicationContext(), "File downloaded successfully", Toast.LENGTH_LONG).show();
                } catch (Exception ex) {
                	ex.printStackTrace();
					System.out.println(ex.getMessage());
                }
            }
            
        } catch (IOException ex) {
        	ex.printStackTrace();
			System.out.println(ex.getMessage());

            if (conn.getResponseCode() == 500) {
                byte[] bytes = new byte[4096];
                int numBytes = conn.getErrorStream().read(bytes);
                String errorMsg = new String(bytes, 0, numBytes);
                // try to extract the text in the h1 tag
                int pos1 = errorMsg.indexOf("<h1>");
                if (pos1 >= 0) {
                    errorMsg = errorMsg.substring(pos1 + 4);
                    int pos2 = errorMsg.indexOf("</h1>");
                    if (pos2 >= 0) {
                        errorMsg = errorMsg.substring(0, pos2);
                    }
                }
                // keep the stack trace from the original exception
                final IOException ex2 = new IOException(errorMsg);
                ex2.setStackTrace(ex.getStackTrace());
                throw ex2;
            }
        }
        return file2;
    }
    
    public void deleteCache(Context context) {
        try {
            File dir = context.getCacheDir();//          /data/data/com.docuvantage.andhelper.main/cache
            if (dir != null && dir.isDirectory()) {
                deleteDir(dir);
            }
        } catch (Exception e) {
        	e.printStackTrace();
			System.out.println(e.getMessage());
        }
    }

    public boolean deleteDir(File dir) {
        if (dir != null && dir.isDirectory()) {
            String[] children = dir.list();
            for (int i = 0; i < children.length; i++) {
                boolean success = deleteDir(new File(dir, children[i]));
                if (!success) {
                    return false;
                }
            }
        }
        return dir.delete();
    }
    public boolean deleteDirectory(File path) {
        if( path.exists() ) {
          File[] files = path.listFiles();
          if (files == null) {
              return true;
          }
          for(int i=0; i<files.length; i++) {
             if(files[i].isDirectory()) {
               deleteDirectory(files[i]);
             }
             else {
               files[i].delete();
             }
          }
        }
        return( path.delete() );
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
   
}