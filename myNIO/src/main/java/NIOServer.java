import com.sun.xml.internal.ws.policy.privateutil.PolicyUtils;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * @author jay
 * @date 2019/3/19 23:29
 */
public class NIOServer {

    private ServerSocketChannel serverSocketChannel;
    private Selector selector;


    public NIOServer() throws IOException {
        //打开Server socket channel
        serverSocketChannel  = ServerSocketChannel.open();

        //配置为非阻塞
        serverSocketChannel.configureBlocking(false);

        //绑定 Server port
        serverSocketChannel.socket().bind(new InetSocketAddress(8080));

        //创建selector
        selector = Selector.open();

        //注册channel到selector
        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);

        System.out.println("server 启动");
    }

    private void handleKeys() throws IOException {
        while (true){
            //通过selector选择channel
            int selectNums = selector.select(30 * 1000L);
            if (selectNums == 0){
                continue;
            }
            System.out.println("选择channel的数量:"+selectNums);

            //遍历可选择的channel的selectionKey集合
            Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
            while (iterator.hasNext()){
                SelectionKey selectionKey = iterator.next();
                //移除要处理的key
                iterator.remove();
                //忽略无效的key
                if(!selectionKey.isValid()){
                    continue;
                }
                handleKey(selectionKey);
            }
        }
    }

    /**
     * 针对不同的key进行处理
     * @param selectionKey
     */
    private void handleKey(SelectionKey selectionKey) throws IOException{
        //接收连接就绪的
        if (selectionKey.isAcceptable()){
            handleAcceptableKey(selectionKey);
        }
        //读就绪
        if (selectionKey.isAcceptable()){
            handleReadKey(selectionKey);
        }
        //写就绪
        if (selectionKey.isAcceptable()){
            handleWriteKey(selectionKey);
        }
    }

    private void handleReadKey(SelectionKey selectionKey) throws IOException {
        SocketChannel channel = (SocketChannel)selectionKey.channel();
        ByteBuffer readBuffer = CodeUtil.read(channel);
        //处理连接已断开的情况
        if (readBuffer == null){
            System.out.println("连接已断开");
            channel.register(selector,0);
            return;
        }
        if (readBuffer.position() >0){
            String newString = CodeUtil.newString(readBuffer);
            System.out.println("读取数据:"+newString);
            channel.register(selector,SelectionKey.OP_WRITE,selectionKey.attachment());
        }

    }

    private void handleAcceptableKey(SelectionKey selectionKey) throws IOException {
        //接收client的socket channel
        SocketChannel socketChannel = ((ServerSocketChannel) selectionKey.channel()).accept();
        //设置为非阻塞
        socketChannel.configureBlocking(false);
        System.out.println("接收新的channel");
        //注册到选择器
        socketChannel.register(selector,SelectionKey.OP_READ,new ArrayList<String>());
    }

    @SuppressWarnings("Duplicates")
    private void handleWriteKey(SelectionKey selectionKey) throws IOException{
        SocketChannel channel = (SocketChannel) selectionKey.channel();
        //相应队列
        ArrayList<String> arrayList = (ArrayList<String>) selectionKey.attachment();
        for (String content : arrayList){
            System.out.println("写入的数据："+content);
            CodeUtil.write(channel,content);
        }
        arrayList.clear();
        channel.register(selector,SelectionKey.OP_READ,arrayList);
    }

    public static void main(String[] args) throws IOException {
        NIOServer server = new NIOServer();
    }
}
