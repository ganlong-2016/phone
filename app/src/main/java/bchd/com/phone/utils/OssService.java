package bchd.com.phone.utils;

import java.io.File;
import java.io.InputStream;
import java.util.UUID;

import com.aliyun.oss.OSSClient;


public class OssService {

    private final String bucketName = "daorongoss01";
    private final String endpoint = "oss-cn-shanghai.aliyuncs.com";
    private final String accessKeyId = "LTAIX4Fbg8shLCPW";
    private final String accessKeySecret = "dm9wmxyZlfUUflqLcJeiOUON63BDc1";

    public static class UploadFileException extends Exception {
        UploadFileException(String message) {
            super(message);
        }
    }

    /**
     * 保存oos
     *
     * @param file file
     * @return
     */
    public String upload(File file) throws UploadFileException {
        if (file == null) {
            throw new UploadFileException("文件格式错误");
        }
        String fileName = file.getName();
        String operateName = UUID.randomUUID() + fileName.substring(fileName.lastIndexOf("."), fileName.length());

        try {
            uploadOss(operateName, file);
        } catch (Exception e) {
            throw new UploadFileException("上传失败");
        }
        return "https://" + bucketName + "." + endpoint + "/" + operateName;
    }


    private void uploadOss(String key, File localFilePath) throws UploadFileException {
        OSSClient client = null;
        try {
            client = new OSSClient(endpoint, accessKeyId, accessKeySecret);
            client.putObject(bucketName, key, localFilePath);
        } catch (Exception e) {
            throw new UploadFileException("上传失败");
        } finally {
            if (client != null) {
                client.shutdown();
            }
        }
    }

//    public static void main(String[] args){
//    	//得到文件
//    	File file = new File("F:/Kalimba.mp3");
//    	//new 对象
//    	OssService oss = new OssService();
//    	String ossPath = null;
//    	try {
//    		//上传
//    		ossPath = oss.upload(file);
//    		System.out.println(ossPath);
//		} catch (UploadFileException e) {
//			e.printStackTrace();
//		}
//
//    }
}
