package utils;

import static org.testng.Assert.ARRAY_MISMATCH_TEMPLATE;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.CanceledException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidConfigurationException;
import org.eclipse.jgit.api.errors.InvalidRemoteException;
import org.eclipse.jgit.api.errors.NoHeadException;
import org.eclipse.jgit.api.errors.RefNotAdvertisedException;
import org.eclipse.jgit.api.errors.RefNotFoundException;
import org.eclipse.jgit.api.errors.TransportException;
import org.eclipse.jgit.api.errors.WrongRepositoryStateException;
import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.test.framework.ExcelMeta;
import com.test.framework.LogProxyFactory;
import com.yzt.service.Context;
import com.yzt.service.ServiceFactory;

import contants.Contants;
import exception.customsException;

/**
 * 公共方法
 *
 * @author vivi.zhang
 */
public class CommonUtils {

    public static String MapToJsonString(Map<String, Object> paramsMap) {
        return JSONObject.toJSONString(paramsMap);
    }

    public static Map<String, Object> JsonStringToMap(String data) {
        Map<String, Object> paramsMap = Maps.newHashMap();
        JSONObject redult = JSONObject.parseObject(data);
        for (Entry<String, Object> entry : redult.entrySet()) {
            paramsMap.put(entry.getKey(), entry.getValue());
        }
        return paramsMap;
    }

    /**
     * Excel中读出的数据转换成带key=inputJson的map格式，value=json
     * string，exp：{inputJson:{xxx}}
     *
     * @param data
     * @return
     */
    public static Map<String, Object> inputJsonToMap(String data) {
        Map<String, Object> paramsMap = Maps.newHashMap();
        String value = "";
        String input = "";
        StringBuffer sb = new StringBuffer();
        Pattern pattern = Pattern.compile("inputJson=([\\s\\S]*)[\\}\\]]+");
        Matcher matcher = pattern.matcher(data);
        if (matcher.find()) {
            value = matcher.group(1);
        }
        if (!StringUtils.isEmpty(value)) {
            if (value.startsWith("\\{")) {
                input = sb.append("{").append(value).append("}").toString();
            } else if (value.startsWith("\\[")) {
                input = sb.append("[").append(value).append("]").toString();
            }
            paramsMap.put(Contants.INPUT, input);
        }
        return paramsMap;
    }

    public static List JsonStringToList(String data) {
        List list = Lists.newArrayList();
        JSONArray redult = JSON.parseArray(data);
        Iterator<Object> array = (Iterator<Object>) redult.iterator();
        while (((Iterator<Object>) array).hasNext()) {
            list.add(array.next());
        }
        return list;
    }

    public static JSONObject MapToJsonObject(Map<String, Object> paramsMap) {
        JSONObject jsonObject = new JSONObject();
        for (Entry<String, Object> entry : paramsMap.entrySet()) {
            jsonObject.put(entry.getKey(), entry.getValue());
        }
        return jsonObject;
    }

    public static Object[][] copyArrays(Object[][] objects) {
        int rowNum = 0;
        int columnNum = 0;
        if (objects != null) {
            for (int i = 0; i < objects.length; i++) {
                if (objects[i].length == 0 || objects[i][0] == null) {
                    continue;
                }
                rowNum++;
            }
        }
        columnNum = objects[rowNum - 1].length;
        Object[][] copyed = new Object[rowNum][columnNum];
        for (int i = 0; i < rowNum; i++) {
            for (int j = 0; j < columnNum; j++) {
                copyed[i][j] = objects[i][j];
            }
        }
        return copyed;
    }

    /**
     * 测试用例参数格式转换公共方法
     *
     * @param inputJson 入参：{name=login, inputJson={ "mobile": "13000000008", "passwd":
     *                  "13000000008" }} 返回：{ "mobile": "13000000008", "passwd":
     *                  "13000000008" }
     * @return
     */
    public static String getInputJsonParam(String inputJson) {
        Map<String, Object> input = Maps.newHashMap();
        input.putAll(CommonUtils.inputJsonToMap(inputJson));
        if (input.containsKey(Contants.INPUT)) {
            return (String) input.get(Contants.INPUT);
        } else {
            throw new customsException("数据表中无inputJson数据头，请确认！");
        }
    }

    /**
     * inputJson 格式:{name=login, inputJson={ "mobile": "13000000008", "passwd":
     * "13000000008" }}
     *
     * @param inputJson
     * @return Map<String   ,       String>
     */
    public static Map<String, String> stringToMap(String inputJson) {
        Map<String, String> input = Maps.newHashMap();
        inputJson = inputJson.substring(1, inputJson.length() - 1);
        String[] tokens = StringUtils.split(inputJson, ",");
        for (String string : tokens) {
            if (string.contains("=")) {
                String[] values = StringUtils.split(string, "=");
                input.put(values[0], values[1]);
            }
        }
        return input;
    }

    /**
     * jsonArray转成List
     *
     * @param jsonArray
     * @return
     */
    public static List<String> jsonArrayToList(String jsonArray) {
        List<String> list = Lists.newArrayList();
        JSONArray array = JSON.parseArray(jsonArray);
        for (int i = 0; i < array.size(); i++) {
            list.add(array.getString(i));
        }
        return list;
    }

    /**
     * 获取Excel数据
     *
     * @param classname
     * @param methodname
     * @return
     */
    public static Object[][] getExcelData(String classname, String methodname) {
        Object[][] objects = new Object[1024][1];
        int i = 0;
        DataProviders data = new DataProviders(classname, methodname);
        while (data.hasNext()) {
            objects[i][0] = data.next().toString();
            i++;
        }
        return CommonUtils.copyArrays(objects);
    }

    /**
     * 获取Excel数据
     *
     * @param classname
     * @return
     */
    public static List<Map<String, String>> getExcelData(String classname) {
        List<Map<String, String>> lists = Lists.newArrayList();
        DataProviders data = new DataProviders(classname);
        while (data.hasNext()) {
            lists.add(data.next());
        }
        return lists;
    }

    /**
     * 把Excel获取的数据转成testng的数据
     *
     * @param classname
     * @param method
     * @return
     */
    public static Object[][] getTestNGData(String classname, Method method) {
        Object[][] result = new Object[1024][1];
        int i = 0;
        List<Map<String, String>> lists = Lists.newArrayList();
        lists = CommonUtils.getExcelData(classname);
        for (Map<String, String> map : lists) {
            for (String key : map.keySet()) {
                if (key.equals(ExcelMeta.TESTFUNCTIONNAME.getName()) && map.get(key).equals(method.getName())) {
                    result[i][0] = map.get(ExcelMeta.INPUTJSON.getName());
                    i++;
                }
            }
        }
        return CommonUtils.copyArrays(result);
    }

    public static Object getServiceProxyInstance(Class clazz) {
        LogProxyFactory logProxyFactory = new LogProxyFactory();
        return logProxyFactory.getProxyInstance(clazz);
    }

}
