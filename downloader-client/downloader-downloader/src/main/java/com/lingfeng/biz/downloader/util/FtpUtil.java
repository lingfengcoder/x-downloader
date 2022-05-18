package com.lingfeng.biz.downloader.util;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPListParseEngine;
import org.apache.commons.net.ftp.FTPReply;

import java.io.*;
import java.util.ArrayList;
import java.util.Date;

@Slf4j
public class FtpUtil {

    private FTPClient ftpclient;
    private String ipAddress;
    private int ipPort;
    private String userName;
    private String passWord;

    public FtpUtil(String ip, int port, String username, String password) {
        try {
            ipAddress = new String(ip);
            ipPort = port;
            ftpclient = new FTPClient();
            ftpclient.setDefaultPort(ipPort);
            userName = new String(username);
            passWord = new String(password);

            //设置超时时间
            ftpclient.setConnectTimeout(10 * 1000);
            ftpclient.connect(ipAddress);
            ftpclient.setDataTimeout(60 * 1000);
            //ftpclient.setDefaultTimeout(10*1000);//对于 FTPClient 而言，setDefaultTimeout() 超时的工作跟 setSoTimeout() 是相同的，区别仅在于后者会覆盖掉前者设置的值。
            ftpclient.setSoTimeout(30 * 1000);
            //Linux配置
			/*FTPClientConfig ftpClientConfig = new FTPClientConfig(FTPClientConfig.SYST_UNIX);
			ftpclient.configure(ftpClientConfig);*/
            //设置连接超时时间，超过该时间将发送NoOp命令激活链接(此方式仅用于一下方式)
		    /*retrieveFile(String, OutputStream)
		    appendFile(String, InputStream)
		    storeFile(String, InputStream)
		    storeUniqueFile(InputStream)
		    storeUniqueFileStream(String)*/
//			ftpclient.setControlKeepAliveTimeout(300);//单位为秒,5min
//			ftpclient.setControlKeepAliveReplyTimeout(10*1000);
//			ftpclient.setKeepAlive(true);

            //设置连接超时时间，可用于retrieveFileStream(String)，storeFileStream(String) 以及其他xxxFileStream方法，设置监听
		   /* ftpclient.setCopyStreamListener(new CopyStreamListener() {
		    	private long megsTotal = 0;
				@Override
	            public void bytesTransferred(long totalBytesTransferred,int bytesTransferred, long streamSize) {
					 log.info("下载进度监听======totalBytesTransferred:" + totalBytesTransferred
				                + ", bytesTransferred:" + bytesTransferred + ", streamSize:" + streamSize);
	                long megs = totalBytesTransferred / 1000000;
	                for (long l = megsTotal; l < megs; l++) {
	                    System.err.print("######");
	                    log.error("######");
	                    logout();
	                }
	                megsTotal = megs;
	            }
				@Override
				public void bytesTransferred(CopyStreamEvent event) {
					log.info("事件监听======CopyStreamEvent:"+event);
					 bytesTransferred(event.getTotalBytesTransferred(), event.getBytesTransferred(), event.getStreamSize());

				}
			});*/

			/*ftpclient.setCopyStreamListener(new CopyStreamListener() {

				@Override
				public void bytesTransferred(long totalBytesTransferred, int bytesTransferred, long streamSize) {
			        log.info("下载进度监听======totalBytesTransferred:" + totalBytesTransferred
			                + ", bytesTransferred:" + bytesTransferred + ", streamSize:" + streamSize);
			        log.info("监听进度======"+((streamSize+bytesTransferred)/totalBytesTransferred));

				}

				@Override
				public void bytesTransferred(CopyStreamEvent event) {
					log.info("下载事件监听======CopyStreamEvent:"+event);
					bytesTransferred(event.getTotalBytesTransferred(), event.getBytesTransferred(), event.getStreamSize());

				}
			});*/

//			if (SafeUtils.getInteger(javacommon.util.SafeUtils.getEnterLocalModel()) == 0) {
//				ftpclient.enterLocalActiveMode();//client主动模式
//				//ftpclient.enterRemoteActiveMode(host,port); //server to server主动模式  ,host - 被动模式服务器接受数据传输的连接。port - 被动模式服务器的数据端口。(被动服务器指的是该服务器的对端服务器，一般server to server为一主动一被动)
//			} else {
            ftpclient.enterLocalPassiveMode();//client被动模式
//				//ftpclient.enterRemotePassiveMode();//server to server被动模式
//			}
            //取消服务器获取自身Ip地址和提交的host进行匹配，否则当不一致时会报异常。
            //ftpclient.setRemoteVerificationEnabled(true);
            //设置将过程中使用到的命令输出到控制台
            //ftpclient.addProtocolCommandListener(new PrintCommandListener(new PrintWriter(System.out)));
        } catch (Exception e) {
            log.error(e.getMessage());
        }

    }

    public long getFileSize(String fullFilePath) {
        try {
            FTPListParseEngine engine = ftpclient.initiateListParsing(fullFilePath);
            while (engine.hasNext()) {
                FTPFile[] files = engine.getNext(5);
                for (int i = 0; i < files.length; i++) {
                    long size = files[i].getSize();
                    return size;
                }
            }
        } catch (IOException ex) {
            log.error("getFileSize:" + ex.getMessage(), ex);
        }
        return 0L;
    }

    public boolean loginOfTime() {
        boolean error = false;
        try {
            ftpclient.setControlEncoding("gb2312");

            ftpclient.login(userName, passWord);

            //判断是否连接成功
            int reply = ftpclient.getReplyCode();
            if (!FTPReply.isPositiveCompletion(reply)) {
                ftpclient.disconnect();
                log.info("FTP server refused connection.");
                return true;
            }
            ftpclient.setConnectTimeout(10 * 1000);       //连接超时为1min
            ftpclient.setDataTimeout(60 * 1000);       //设置传输超时时间为2h
//			if(SafeUtils.getInteger(javacommon.util.SafeUtils.getEnterLocalModel())==0){
//				ftpclient.enterLocalActiveMode();//主动模式
//			}else{
            ftpclient.enterLocalPassiveMode();//被动模式
//			}
            ftpclient.setFileType(FTPClient.BINARY_FILE_TYPE);
            ftpclient.setFileTransferMode(FTPClient.STREAM_TRANSFER_MODE);

        } catch (Exception ex) {
            log.error("loginOfTime:" + ex.getMessage(), ex.fillInStackTrace());
            ex.printStackTrace();
            error = true;
        }
        return error;
    }

    public boolean loginOfTimes(int times) {
        for (int i = 1; i <= times; i++) {
            log.info("第" + i + "次登录！");
            boolean flag = loginOfTime();
            if (i == times) {
                return flag;
            } else if (!flag) {
                return flag;
            }
        }
        return loginOfTime();
    }

    public void logout() {
        //用ftpclient.closeServer()断开FTP出错时用下更语句退出
        //ftpclient.logout();
        try {
            if (ftpclient != null && ftpclient.isConnected()) {
//				ftpclient.logout();
                log.info("logout success!");
                ftpclient.disconnect();
                log.info("disconnect success!");
            }
        } catch (Exception e) {
            log.error("logout error");
            e.printStackTrace();
            log.error(e.getMessage());
        }
    }

    /**
     * 在FTP服务器上建立指定的目录,当目录已经存在的情下不会影响目录下的文件,这样用以判断FTP
     * 上传文件时保证目录的存在目录格式必须以"/"根目录开头
     *
     * @param pathList String
     * @throws Exception
     */
    public void buildList(String pathList) throws Exception {
        pathList = checkName(pathList);
        String[] paths = pathList.split("/");
        for (int i = 0; i < paths.length; i++) {
            ftpclient.makeDirectory(paths[i]);
            ftpclient.changeWorkingDirectory(paths[i]);
        }
    }

    /**
     * 取得指定目录下的所有文件名，不包括目录名称
     * 分析nameList得到的输入流中的数，得到指定目录下的所有文件名
     *
     * @param fullPath String
     * @return ArrayList
     * @throws Exception
     */
    public boolean isExistFilePath(String fullPath) throws Exception {
        String parentPath = fullPath.substring(0, fullPath.lastIndexOf('/'));
        String pathName = fullPath.substring(fullPath.lastIndexOf('/') + 1);
        parentPath = checkName(parentPath);
        ArrayList namesList = getDirs(parentPath);
        return namesList.contains(pathName);
    }

    /**
     * 取得指定目录下的所有文件名，不包括目录名称
     * 分析nameList得到的输入流中的数，得到指定目录下的所有文件名
     *
     * @param fullName String
     * @return ArrayList
     * @throws Exception
     */
    public boolean isExistFileName(String fullName) throws Exception {
        String parentPath = fullName.substring(0, fullName.lastIndexOf('/'));
        String pathName = fullName.substring(fullName.lastIndexOf('/') + 1);
        parentPath = checkName(parentPath);
        ArrayList namesList = fileNames(parentPath);
        return namesList.contains(pathName);
    }

    /**
     * 取得指定目录下的所有文件名，不包括目录名称
     * 分析nameList得到的输入流中的数，得到指定目录下的所有文件名
     *
     * @param fullPath String
     * @return ArrayList
     * @throws Exception
     */
    public ArrayList getDirs(String fullPath) throws Exception {
        fullPath = checkName(fullPath);
        ArrayList namesList = new ArrayList();
        FTPFile[] names = ftpclient.listDirectories(fullPath);
        if (names != null) {
            for (int i = 0; i < names.length; i++) {
                namesList.add(names[i].getName());
            }
        }
        return namesList;
    }

    /**
     * 取得指定目录下的所有文件名，不包括目录名称
     * 分析nameList得到的输入流中的数，得到指定目录下的所有文件名
     *
     * @param fullPath String
     * @return ArrayList
     * @throws Exception
     */
    public ArrayList fileNames(String fullPath) throws Exception {
        fullPath = checkName(fullPath);
        ArrayList namesList = new ArrayList();
        String[] names = ftpclient.listNames(fullPath);
        if (names != null) {
            for (int i = 0; i < names.length; i++) {
                namesList.add(names[i]);
            }
        }
        return namesList;
    }

    public int delFileName(String filePath) throws Exception {
        try {
            filePath = checkName(filePath);
            ftpclient.deleteFile(filePath);
        } catch (Exception e) {

        }
        return 0;
    }

    public boolean upFtpFile(String source, String destination, Long fileSize, Long offset) throws IOException {
        destination = checkName(destination);
        boolean error = false;
        OutputStream ftpOut = null;
        BufferedOutputStream bos = null;
        RandomAccessFile accessFile = null;
        byte[] b = null;
        try {
            if (destination.split("/").length > 2) {
                if (!isExistFilePath(destination.substring(0, destination.lastIndexOf('/')))) {
                    buildList(destination.substring(0, destination.lastIndexOf('/')));
                }
            }
            Long buffer = 1024 * 8L;
            ftpclient.setBufferSize(buffer.intValue());
            accessFile = new RandomAccessFile(source, "rw");
            if (isExistFileName(destination)) {
                //断点续传
                if (offset != null && offset.equals(fileSize)) {
                    log.info("上传文件已存在======source:" + source + "; destination:" + destination + "; fileSize:" + fileSize);
                    return error;
                } else if (offset != null && offset > fileSize) {
                    offset = 0L;
                    boolean deleteFlag = ftpclient.deleteFile(destination);
                    log.info("上传文件时，目标ftp文件大于本地文件，删除目标ftp文件======result:" + deleteFlag + "; source:" + source + "; destination:" + destination + "; fileSize:" + fileSize);
                }
            } else {
                offset = 0L;
            }
            if (offset > 0) {
                ftpOut = ftpclient.appendFileStream(destination);
                ftpclient.setRestartOffset(offset);//out其在之后设置offset
                accessFile.seek(offset);
            } else {
                ftpOut = ftpclient.storeFileStream(destination);
            }
            bos = new BufferedOutputStream(ftpOut);
            // 显示进度的上传
            long localreadbytes = 0L;
            long lastreadbytes = 0L;
            long lastreadtime = new Date().getTime();

            if (offset > 0) {
                localreadbytes = offset;
                lastreadbytes = offset;
            }

            if (buffer >= fileSize) {
                b = new byte[fileSize.intValue()];
                int len = b.length;
                int readBytes = 0;
                while (readBytes < len) {
                    int readByte = accessFile.read(b, readBytes, len - readBytes);
                    if (readByte == -1) {
                        break;
                    }
                    readBytes += readByte;
                }
                bos.write(b);
            } else {
                b = new byte[buffer.intValue()];
                int readByte = 0;
                while ((readByte = accessFile.read(b)) != -1) {
                    if (ftpOut == null) {
                        log.error("ftp 断开连接了...");
                        log.error("ftp 尝试重新连接...");
                        if (offset > 0) {
                            ftpOut = ftpclient.appendFileStream(destination);
                            ftpclient.setRestartOffset(offset);//out其在之后设置offset
                            accessFile.seek(offset);
                        } else {
                            ftpOut = ftpclient.storeFileStream(destination);
                        }
                        bos = new BufferedOutputStream(ftpOut);
                        log.error("ftp 重新连接了...");
                    }
                    bos.write(b, 0, readByte);
                    localreadbytes += readByte;
                    try {
                        long localreadtime = new Date().getTime();
                        long readtimeperiod = localreadtime - lastreadtime;
                        if (readtimeperiod >= 10 * 1000 || localreadbytes == fileSize) {//10秒或者完成
                            //process = localreadbytes / step;
                            long readbytesperiod = localreadbytes - lastreadbytes;
                            lastreadbytes = localreadbytes;
                            lastreadtime = localreadtime;
                            String tmpspeed = getSpeed(readbytesperiod, readtimeperiod);
                            String tmpprocess = getProcess(localreadbytes, fileSize);
                            log.info("*******上传速度及进度:" + destination + "======" + tmpspeed + "B/s" + "======" + tmpprocess + "%");
                        }
                    } catch (Exception e) {
                        log.error("获取上传速度及进度异常！");
                        log.error(e.getMessage());
                        e.printStackTrace();
                    }
                }

            }
            //程序为缓存写入文件进行线程挂起等待
			/*try {
				if(fileSize/1024/2<1000){
					Thread.sleep(1000);
				}else{
					Thread.sleep(fileSize/1024/2);
				}
			} catch (Exception e) {
				// TODO: handle exception
			}*/
        } catch (Exception ex) {
            error = true;
//			log.error("upFtpFile:"+ex.getMessage(), ex.fillInStackTrace());
            ex.printStackTrace();
        } finally {
            try {
                if (bos != null) {
                    bos.flush();
                    accessFile.close();
                    bos.close();
                    error = !ftpclient.completePendingCommand();
                }
            } catch (Exception ex) {
                log.error("upFtpFile关闭流异常:" + ex.getMessage(), ex.fillInStackTrace());
                ex.printStackTrace();
            }
        }
        return error;
    }


    public boolean upFtpFileNew(String source, String destination, Long fileSize, Long offset, boolean buildDir) throws IOException {
//		log.info("status+++++++"+ftpclient.getStatus());
        boolean error = false;
        OutputStream ftpOut = null;
        BufferedOutputStream bos = null;
        RandomAccessFile accessFile = null;
        byte[] b = null;
        try {
            if (buildDir && offset == 0) {
                if (destination.split("/").length > 2) {
                    if (!isExistFilePath(destination.substring(0, destination.lastIndexOf('/')))) {
                        buildList(destination.substring(0, destination.lastIndexOf('/')));
                    }
                }
            } else {
                if (destination.split("/").length > 2) {
                    ftpclient.changeWorkingDirectory(destination.substring(0, destination.lastIndexOf("/") + 1));
                } else {
                    ftpclient.changeWorkingDirectory(destination);
                }
            }
            Long buffer = 1024 * 8L;
            ftpclient.setBufferSize(buffer.intValue());
            accessFile = new RandomAccessFile(source, "rw");
			/*if (isExistFileName(destination)) {

			}else{
				offset = 0L;
			}*/
            if (offset > 0) {
                //断点续传
                if (offset != null && offset.equals(fileSize)) {
                    log.info("上传文件已存在======source:" + source + "; destination:" + destination + "; fileSize:" + fileSize);
                    return error;
                } else if (offset != null && offset > fileSize) {
                    offset = 0L;
                    boolean deleteFlag = ftpclient.deleteFile(destination);
                    if (!deleteFlag) {
                        log.info("上传文件时，目标ftp文件大于本地文件，删除目标ftp文件======result:" + deleteFlag + "; source:" + source + "; destination:" + destination + "; fileSize:" + fileSize);
                        return true;
                    }
                }
                ftpOut = ftpclient.appendFileStream(destination);
                ftpclient.setRestartOffset(offset);//out其在之后设置offset
                accessFile.seek(offset);
            } else {
                ftpOut = ftpclient.storeFileStream(destination);
            }
            bos = new BufferedOutputStream(ftpOut);
            // 显示进度的上传
            long localreadbytes = 0L;
            long lastreadbytes = 0L;
            long lastreadtime = new Date().getTime();

            if (offset > 0) {
                localreadbytes = offset;
                lastreadbytes = offset;
            }
            if (buffer >= fileSize) {
                b = new byte[fileSize.intValue()];
                int len = b.length;
                int readBytes = 0;
                while (readBytes < len) {
                    int readByte = accessFile.read(b, readBytes, len - readBytes);
                    if (readByte == -1) {
                        break;
                    }
                    readBytes += readByte;
                }
                bos.write(b);
            } else {
                b = new byte[buffer.intValue()];
                int readByte = 0;
                while ((readByte = accessFile.read(b)) != -1) {
                    if (ftpOut == null) {
                        log.error("ftp 断开连接了...");
                        log.error("ftp 尝试重新连接...");
                        if (offset > 0) {
                            ftpOut = ftpclient.appendFileStream(destination);
                            ftpclient.setRestartOffset(offset);//out其在之后设置offset
                            accessFile.seek(offset);
                        } else {
                            ftpOut = ftpclient.storeFileStream(destination);
                        }
                        bos = new BufferedOutputStream(ftpOut);
                        log.error("ftp 重新连接了...");
                    }
                    bos.write(b, 0, readByte);
                    localreadbytes += readByte;
                    try {
                        long localreadtime = new Date().getTime();
                        long readtimeperiod = localreadtime - lastreadtime;
                        if (readtimeperiod >= 10 * 1000 || localreadbytes == fileSize) {//10秒或者完成
                            //process = localreadbytes / step;
                            long readbytesperiod = localreadbytes - lastreadbytes;
                            lastreadbytes = localreadbytes;
                            lastreadtime = localreadtime;
                            String tmpspeed = getSpeed(readbytesperiod, readtimeperiod);
                            String tmpprocess = getProcess(localreadbytes, fileSize);
                            log.info("*******上传速度及进度:" + destination + "======" + tmpspeed + "B/s" + "======" + tmpprocess + "%");
                        }
                    } catch (Exception e) {
                        log.error("获取上传速度及进度异常！");
                        log.error(e.getMessage());
                        e.printStackTrace();
                    }
                }

            }
            //程序为缓存写入文件进行线程挂起等待
			/*try {
				if(fileSize/1024/2<1000){
					Thread.sleep(1000);
				}else{
					Thread.sleep(fileSize/1024/2);
				}
			} catch (Exception e) {
				// TODO: handle exception
			}*/
        } catch (Exception ex) {
            error = true;
            log.error("upFtpFile:" + ex.getMessage(), ex.fillInStackTrace());
            ex.printStackTrace();
        } finally {
            try {
                if (bos != null) {
                    bos.flush();
                    accessFile.close();
                    bos.close();
                }
            } catch (Exception ex) {
                log.error("upFtpFile关闭流异常:" + ex.getMessage(), ex.fillInStackTrace());
                ex.printStackTrace();
            } finally {
                error = !ftpclient.completePendingCommand();
            }
        }
        return error;
    }


    /**
     * 上传文件到FTP服务器,destination路径以FTP服务器的"/"开始，带文件名、
     * 上传文件只能使用二进制模式，当文件存在时再次上传则会覆盖
     *
     * @param source      本地路径
     * @param destination 远程ftp路径
     * @throws Exception 2017.3.25 增加异常日志输出
     */
    public boolean upFtpFile2(String source, String destination, Long fileSize, Long offset) {
        destination = checkName(destination);
        boolean error = false;
        OutputStream ftpOut = null;
        BufferedInputStream bufferIn = null;
        FileInputStream ftpIn = null;
        try {
            if (destination.split("/").length > 2) {

                if (!isExistFilePath(destination.substring(0, destination.lastIndexOf('/')))) {
                    buildList(destination.substring(0, destination.lastIndexOf('/')));
                }
            }
            if (offset != null && offset.equals(fileSize)) {
                return error;
            }
//        	FileInputStream ftpIn = new FileInputStream(source);
//        	ftpclient.storeFile(destination, ftpIn);
            Long buffer = 1024 * 10L;
            ftpclient.setBufferSize(buffer.intValue());
            ftpIn = new FileInputStream(source);

            boolean flag = false;
            if (isExistFileName(destination)) {
                flag = ftpclient.deleteFile(destination);
            }
            try {
                ftpOut = ftpclient.storeFileStream(destination);
            } catch (Exception ex) {
                log.error("ftpOut:" + destination + ";" + ex.getMessage(), ex);
            }

            bufferIn = new BufferedInputStream(ftpIn, buffer.intValue());

            // 显示进度的上传
            long step = fileSize / 100;
            long process = 0;
            long localreadbytes = 0L;
            if (offset > 0) {
                localreadbytes = offset;
            }

            int readByte = 0;
            bufferIn.skip(offset);
            byte[] b = new byte[buffer.intValue()];
            while ((readByte = bufferIn.read(b)) > 0) {
                ftpOut.write(b, 0, readByte);
                localreadbytes += readByte;
                if (localreadbytes / step != process) {
                    process = localreadbytes / step;
                    log.info("上传进度:" + destination + "==" + process + "%");
                }
            }
        } catch (Exception ex) {
            log.error("upFtpFile2:" + ex.getMessage(), ex);
            error = true;
        } finally {//必须要在最后进行流关闭，否则异常，会出现不会关闭流的情况
            try {
                ftpOut.close();
                bufferIn.close();
                ftpIn.close();
            } catch (Exception e) {
                log.error("关闭流异常:" + e.getMessage(), e);
            }
        }
        return error;
    }


    /** */
    /**
     * 上传文件到服务器,新上传和断点续传
     *
     * @param remoteFile 远程文件名，在上传之前已经将服务器工作目录做了改变
     * @param localFile  本地文件 File句柄，绝对路径
     * @return
     * @throws IOException
     * @since processStep 需要显示的处理进度步进值
     */
    public boolean uploadFile(String remoteFile, File localFile, Long fileSize, Long remoteSize) {
        boolean error = false;
        try {
            if (remoteSize != null && remoteSize.equals(fileSize)) {
                return error;
            }
            String destination = checkName(remoteFile);
            if (destination.split("/").length > 2) {
                if (!isExistFilePath(destination.substring(0, destination.lastIndexOf('/')))) {
                    buildList(destination.substring(0, destination.lastIndexOf('/')));
                }
            }
            if (remoteSize > fileSize) {
                boolean flag = ftpclient.deleteFile(destination);
                if (flag) {
                    remoteSize = 0L;
                }
            }
            if (remoteSize.equals(fileSize)) {
                return false;
            }
            // 显示进度的上传
            long step = localFile.length() / 100;
            long process = 0;
            long localreadbytes = 0L;
            RandomAccessFile raf = new RandomAccessFile(localFile, "r");
            OutputStream out = ftpclient.appendFileStream(remoteFile);
            // 断点续传
            if (remoteSize > 0) {
                ftpclient.setRestartOffset(remoteSize);
                process = remoteSize / step;
                raf.seek(remoteSize);
                localreadbytes = remoteSize;
            }
            byte[] bytes = new byte[1024];
            int readByte = 0;
            while ((readByte = raf.read(bytes)) != -1) {
                out.write(bytes, 0, readByte);
                localreadbytes += readByte;
                if (localreadbytes / step != process) {
                    process = localreadbytes / step;
                    log.info("上传进度:" + destination + "==" + process);
                }
            }
            //out.flush();
            raf.close();
            out.close();
            boolean result = ftpclient.completePendingCommand();
            error = !result;
        } catch (Exception ex) {
            log.error("uploadFile:" + ex.getMessage(), ex);
        }


        return error;
    }

    /**
     * 上传文件到FTP服务器,destination路径以FTP服务器的"/"开始，带文件名、
     * 上传文件只能使用二进制模式，当文件存在时再次上传则会覆盖
     *
     * @param source      String
     * @param destination String
     * @throws Exception
     */
    public void upFile(String source, String destination) throws Exception {
        destination = checkName(destination);
        if (destination.split("/").length > 2) {
            if (!isExistFilePath(destination.substring(0, destination.lastIndexOf('/')))) {
                buildList(destination.substring(0, destination.lastIndexOf('/')));
            }
        }
        FileInputStream ftpIn = new FileInputStream(source);
        ftpclient.storeFile(destination, ftpIn);

//    	OutputStream ftpOut = ftpclient.storeFileStream(destination);
//    	FileInputStream ftpIn = new FileInputStream(source);
//    	byte[] buf = new byte[ftpIn.available()];
//        ftpIn.read(buf);
//        ftpOut.write(buf);
//        ftpIn.close();
//        ftpOut.close();

    }


    /**
     * JSP中的流上传到FTP服务器,
     * 上传文件只能使用二进制模式，当文件存在时再次上传则会覆盖
     * 字节数组做为文件的输入流,此方法适用于JSP中通过
     * request输入流来直接上传文件在RequestUpload类中调用了此方法，
     * destination路径以FTP服务器的"/"开始，带文件名
     *
     * @param sourceData  byte[]
     * @param destination String
     * @throws Exception
     */
    public void upFile(byte[] sourceData, String destination) throws Exception {

        destination = checkName(destination);
        if (destination.split("/").length > 2) {
            if (!isExistFilePath(destination.substring(0, destination.lastIndexOf('/')))) {
                buildList(destination.substring(0, destination.lastIndexOf('/')));
            }
        }

        InputStream ftpIn = new ByteArrayInputStream(sourceData);
        ftpclient.storeFile(destination, ftpIn);


//    	OutputStream ftpOut = ftpclient.storeFileStream(destination);
//    	//ftpclient.storeFile(remote, local);
//    	ftpOut.write(sourceData);
//        ftpOut.close();
//
    }


    public boolean downFile(String sourceFileName, String destinationFileName, Long fileSize, Long offset) throws Exception {
        boolean error = false;
        sourceFileName = checkName(sourceFileName);
        InputStream in = null;
        RandomAccessFile accessFile = null;
        //FileOutputStream accessFile = null;
        BufferedInputStream bis = null;
        byte[] b = null;
        try {
            File file = new File(destinationFileName);
            File parentFile = file.getParentFile();
            if (!parentFile.exists()) {
                parentFile.mkdirs();
            }
            Long buffer = 1024 * 8L;
            ftpclient.setBufferSize(buffer.intValue());
            String filepath = sourceFileName.substring(0, sourceFileName.lastIndexOf("/"));
            if (filepath.equals("")) {
                filepath = "/";
            }
            //ftpclient.changeWorkingDirectory(filepath);
            accessFile = new RandomAccessFile(file, "rw");

            if (offset != null && offset.equals(fileSize)) {
                log.info("下载文件已存在======source:" + sourceFileName + "; destination:" + destinationFileName + "; fileSize:" + fileSize);
                return error;
            } else if (offset != null && offset > fileSize) {
                offset = 0L;
                boolean deleteFlag = file.delete();
                log.info("下载文件时，本地文件大于源ftp文件，删除本地文件======result:" + deleteFlag + "; source:" + sourceFileName + "; destination:" + destinationFileName + "; fileSize:" + fileSize);
            }

            //ftpclient.setCopyStreamListener(createCopyStreamListener());
            //显示下载进度
			/*long step = fileSize / 100;
			long process = 0L;*/
            long localreadbytes = 0L;
            long lastreadbytes = 0L;
            long lastreadtime = new Date().getTime();

            if (offset > 0) {
                localreadbytes = offset;
                lastreadbytes = offset;
                ftpclient.setRestartOffset(offset);//这行才是断点续传！！！in在其之前设置offset
                accessFile.seek(offset);
            }

            in = ftpclient.retrieveFileStream(sourceFileName);
			/*try {

				ftpclient.getReply();// 主动调用一次getReply()把接下来的226消费掉. 这样做是可以解决这个返回null问题
			} catch (Exception e) {
				// TODO: handle exception
			}*/
            bis = new BufferedInputStream(in);

            if (buffer >= fileSize) {
                b = new byte[fileSize.intValue()];
                int len = b.length;
                int readBytes = 0;
                while (readBytes < len) {
                    int readByte = bis.read(b, readBytes, len - readBytes);
                    if (readByte == -1) {
                        break;
                    }
                    readBytes += readByte;
                }
                accessFile.write(b);
            } else {
                b = new byte[buffer.intValue()];
                int readByte = 0;
                while ((readByte = bis.read(b)) != -1) {
                    accessFile.write(b, 0, readByte);
                    localreadbytes += readByte;
                    try {
                        long localreadtime = new Date().getTime();
                        long readtimeperiod = localreadtime - lastreadtime;
                        //ftpclient.getCopyStreamListener().bytesTransferred(localreadbytes, readByte, fileSize);
                        if (readtimeperiod >= 10 * 1000 || localreadbytes == fileSize) {
                            //process = localreadbytes / step;
                            long readbytesperiod = localreadbytes - lastreadbytes;
                            lastreadbytes = localreadbytes;
                            lastreadtime = localreadtime;
                            String tmpspeed = getSpeed(readbytesperiod, readtimeperiod);
                            String tmpprocess = getProcess(localreadbytes, fileSize);
                            log.info("*******下载速度及进度:" + destinationFileName + "======" + tmpspeed + "B/s" + "======" + tmpprocess + "%");
                        }
                    } catch (Exception e) {
                        log.error("获取下载速度及进度异常！");
                        log.error(e.getMessage());
                        e.printStackTrace();
                    }
                }
            }

        } catch (IOException e) {
            error = true;
            log.error("downFile:" + e.getMessage(), e.fillInStackTrace());
            e.printStackTrace();
        } finally {
            try {
                if (bis != null) {
                    bis.close();
                }
            } catch (Exception ex) {
                log.error("downFile关闭BufferedInputStream异常:" + ex.getMessage(), ex.fillInStackTrace());
                ex.printStackTrace();
            }
            try {
                if (in != null) {
                    in.close();
                }
            } catch (Exception ex) {
                log.error("downFile关闭InputStream异常:" + ex.getMessage(), ex.fillInStackTrace());
                ex.printStackTrace();
            }
            try {
                if (accessFile != null) {
                    accessFile.close();
                }
            } catch (Exception ex) {
                log.error("downFile关闭RandomAccessFile异常:" + ex.getMessage(), ex.fillInStackTrace());
                ex.printStackTrace();
            }
            try {
                error = !ftpclient.completePendingCommand();
            } catch (Exception ex) {
                log.error("downFile校验completePendingCommand异常:" + ex.getMessage(), ex.fillInStackTrace());
                ex.printStackTrace();
            }
        }
        return error;
    }

    public String getSpeed(long readBytesPeriod, long readTimePeriod) {
        double speed = readBytesPeriod * 1000.0 / readTimePeriod;
        Integer K = 1024;
        Integer M = K * 1024;
        Integer G = M * 1024;
        String formatSpeed = "";
        if (speed / G >= 1) {
            //BigDecimal bigDecimal = new BigDecimal(speed / G);
            //formatSpeed = bigDecimal.setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() + "G";
            formatSpeed = String.format("%.2f", speed / G) + "G";
        } else if (speed / M >= 1) {
            //BigDecimal bigDecimal = new BigDecimal(speed / M);
            //formatSpeed = bigDecimal.setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() + "M";
            formatSpeed = String.format("%.2f", speed / M) + "M";
        } else if (speed / K >= 1) {
            //BigDecimal bigDecimal = new BigDecimal(speed / K);
            //formatSpeed = bigDecimal.setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() + "K";
            formatSpeed = String.format("%.2f", speed / K) + "K";
        } else {
            formatSpeed = String.format("%.2f", speed);
        }
        return formatSpeed;
    }

    public String getProcess(long readBytes, long fileSize) {
        double process = readBytes * 100.0 / fileSize;
        String formatProcess = String.format("%.2f", process);
        return formatProcess;
    }

    public void downFile(String SourceFileName, String destinationFileName) throws
            Exception {
        SourceFileName = checkName(SourceFileName);
        FileOutputStream byteOut = null;
        try {
            byteOut = new FileOutputStream(destinationFileName);
            ftpclient.retrieveFile(SourceFileName, byteOut);
            byteOut.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 从FTP文件服务器上下载文件，输出到字节数组中
     *
     * @param SourceFileName String
     * @return byte[]
     * @throws Exception
     */
    public byte[] downFile(String SourceFileName, long offset) throws Exception {
        SourceFileName = checkName(SourceFileName);
        ByteArrayOutputStream byteOut = null;
        try {
            byteOut = new ByteArrayOutputStream();
            ftpclient.setRestartOffset(offset);
            ftpclient.retrieveFile(SourceFileName, byteOut);
            byteOut.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return byteOut.toByteArray();
    }

    public byte[] downFile(String SourceFileName) throws Exception {
        SourceFileName = checkName(SourceFileName);
        ByteArrayOutputStream byteOut = null;
        try {
            byteOut = new ByteArrayOutputStream();
            ftpclient.retrieveFile(SourceFileName, byteOut);
            byteOut.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return byteOut.toByteArray();
    }

    public String checkName(String name) {
        if (!name.startsWith("/")) {
            name = "/" + name;
        }
        return name;
    }

    /**
     * 检查文件上的文件状态
     *
     * @param ftpFileURL ftp文件路径 ，如：20170615/cmd/00800000005020170615173940261103.xml
     * @return int
     */
    public int checkFtpFile(String ftpFileURL) {
        int replyCode = 0;
        try {
            replyCode = ftpclient.getReplyCode();
            InputStream inputStream = ftpclient.retrieveFileStream(ftpFileURL);
            if (inputStream == null) {
                replyCode = ftpclient.getReplyCode();
            } else {
                if (ftpclient.getReplyCode() == FTPReply.FILE_UNAVAILABLE) {//文件不可操作或者不存在
                    replyCode = FTPReply.FILE_UNAVAILABLE;
                }
            }
        } catch (Exception e) {
            log.error("checkFtpFile:" + e.getMessage(), e);
        }
        return replyCode;
    }

}
