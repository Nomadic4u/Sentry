package org.example.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import org.example.entity.dto.Client;
import org.example.entity.dto.ClientDetail;
import org.example.entity.dto.ClientSsh;
import org.example.entity.vo.request.*;
import org.example.entity.vo.response.*;
import org.example.mapper.ClientDetailMapper;
import org.example.mapper.ClientMapper;
import org.example.mapper.ClientSshMapper;
import org.example.service.ClientService;
import org.example.utils.InfluxDbUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ClientServiceImpl extends ServiceImpl<ClientMapper, Client> implements ClientService {

    private String registerToken = this.generateNewToken();

    private final Map<Integer, Client> clientIdCache = new ConcurrentHashMap<>();

    private final Map<String, Client> clientTokenCache = new ConcurrentHashMap<>();

    @Resource
    ClientDetailMapper clientDetailMapper;

    @Resource
    ClientSshMapper clientSshMapper;

    @Resource
    InfluxDbUtils influxDbUtils;

    /**
     * 这个方法会在ClientServiceImpl被创建后执行
     */
    @PostConstruct
    public void initClientCache() {
        clientTokenCache.clear();
        clientIdCache.clear(); // 因为add操作只会覆盖, 不会去清空, 需要手动的去清空
        this.list().forEach(this::addClientCache);
    }

    /**
     * 验证和注册token
     * @param token token
     * @return 是否注册成功
     */
    @Override
    public boolean verifyAndRegister(String token) {
        if (this.registerToken.equals(token)) {
            int id = this.randomClientId();
            Client client = new Client(id, "未命名主机", token, "cn", "未命名节点", new Date());
            if (this.save(client)) {
                registerToken = this.generateNewToken(); // 重新生成一下token
                this.addClientCache(client);
                return true;
            }
        }
        return false;
    }

    @Override
    public Client findClientById(int id) {
        return clientIdCache.get(id);
    }

    @Override
    public Client findClientByToken(String token) {
        return clientTokenCache.get(token);
    }

    @Override
    public String registerToken() {
        return registerToken;
    }

    // 更新客户端的信息
    @Override
    public void updateClientDetails(ClientDetailVO vo, Client client) {
        ClientDetail detail = new ClientDetail();
        BeanUtils.copyProperties(vo, detail);
        detail.setId(client.getId());
        if (Objects.nonNull(clientDetailMapper.selectById(client.getId()))) {
            clientDetailMapper.updateById(detail);
        } else {
            clientDetailMapper.insert(detail); // 没有的话就直接插入
        }
    }


    // 存贮最近时间的运行时信息
    private Map<Integer, RuntimeDetailVO> currentRuntime = new ConcurrentHashMap<>();

    // 数据存入influxdb
    @Override
    public void updateRuntimeDetails(RuntimeDetailVO vo, Client client) {
        currentRuntime.put(client.getId(), vo);
        influxDbUtils.writeRuntimeData(client.getId(), vo);
    }

    /**
     * 获取首页卡片列表
     *
     * @return list
     */
    @Override
    public List<ClientPreviewVO> listAllClient() {

        return clientIdCache.values().stream().map(client -> {
            ClientPreviewVO vo = client.asViewObject(ClientPreviewVO.class);
            BeanUtils.copyProperties(clientDetailMapper.selectById(vo.getId()), vo); // 缺失的数据再从数据库中查询
            RuntimeDetailVO runtimeDetailVo = currentRuntime.get(client.getId());
            // 如果最后一次更新的时间大于60秒, 说明已经离线了
            if (runtimeDetailVo != null && System.currentTimeMillis() - runtimeDetailVo.getTimestamp() < 60 * 1000) {
                BeanUtils.copyProperties(runtimeDetailVo, vo);
                vo.setOnline(true);
            }
            return vo;
        }).toList();
    }

    /**
     * 服务器改名
     *
     * @param vo 重命名服务器实体
     */
    @Override
    public void renameClient(RenameClientVO vo) {
        this.update(Wrappers.<Client>update().eq("id", vo.getId()).set("name", vo.getName()));
        this.initClientCache();
    }

    /**
     * 卡片内展示客户端详细信息
     * @param clientId 客户端id
     * @return 客户端详细信息
     */
    @Override
    public ClientDetailsVO clientDetails(int clientId) {
        ClientDetailsVO vo = this.clientIdCache.get(clientId).asViewObject(ClientDetailsVO.class);
        BeanUtils.copyProperties(clientDetailMapper.selectById(clientId), vo);
        vo.setOnline(this.isOnline(currentRuntime.get(clientId)));
        return vo;
    }

    /**
     * 获取服务器近一个小时的数据
     *
     * @param clientId 客户端id
     * @return 客户端近一个小时的数据
     */
    @Override
    public RuntimeHistoryVO clientRuntimeDetailsHistory(int clientId) {
        RuntimeHistoryVO vo = influxDbUtils.readRuntimeData(clientId);
        ClientDetail detail = clientDetailMapper.selectById(clientId);
        BeanUtils.copyProperties(detail, vo);
        return vo;
    }

    /**
     * 获取客户端当前的数据
     *
     * @param clientId 客户端id
     * @return 客户端当前数据
     */
    @Override
    public RuntimeDetailVO clientRuntimeDetailsNow(int clientId) {
        return currentRuntime.get(clientId);
    }

    /**
     * 删除客户端
     * @param clientId 客户端id
     */
    @Override
    public void deleteClient(int clientId) {
        this.removeById(clientId);
        clientDetailMapper.deleteById(clientId);
        this.initClientCache();
        currentRuntime.remove(clientId);
    }

    /**
     * 给子用户分配主机时展示
     * @return 主机列表
     */
    @Override
    public List<ClientSimpleVO> listSimpleList() {
        return clientIdCache.values().stream().map(client -> {
            ClientSimpleVO vo = client.asViewObject(ClientSimpleVO.class);
            BeanUtils.copyProperties(clientDetailMapper.selectById(vo.getId()), vo);
            return vo;
        }).toList();
    }

    /**
     * 保存ssh连接信息
     * @param vo ssh连接实体类
     */
    @Override
    public void saveClientSshConnection(SshConnectionVO vo) {
        Client client = this.clientIdCache.get(vo.getId());
        if (client == null)
            return;
        ClientSsh ssh = new ClientSsh();
        BeanUtils.copyProperties(vo, ssh);
        if (Objects.nonNull(clientSshMapper.selectById(client.getId()))) {
            clientSshMapper.updateById(ssh);
        } else {
            clientSshMapper.insert(ssh);
        }
    }

    /**
     * 查询ssh连接信息
     *
     * @param clientId 用户id
     * @return ssh实体
     */
    @Override
    public SshSettingVO sshSettings(int clientId) {
        ClientDetail detail = clientDetailMapper.selectById(clientId);
        ClientSsh ssh = clientSshMapper.selectById(clientId);
        SshSettingVO vo;
        if (ssh == null) {
            vo = new SshSettingVO();
        } else {
            vo = ssh.asViewObject(SshSettingVO.class);
        }
        vo.setIp(detail.getIp());
        return vo;
    }

    /**
     * 客户端节点重命名
     *
     * @param vo 重命名实体类
     */
    @Override
    public void renameNode(RenameNodeVO vo) {
        this.update(Wrappers.<Client>update().eq("id", vo.getId())
                .set("node", vo.getNode()).set("location", vo.getLocation()));
        this.initClientCache();
    }

    // 判断客户端是否还保持链接
    private boolean isOnline(RuntimeDetailVO runtime) {
        return runtime != null && System.currentTimeMillis() - runtime.getTimestamp() < 60 * 1000;
    }

    private void addClientCache(Client client) {
        clientIdCache.put(client.getId(), client);
        clientTokenCache.put(client.getToken(), client);
    }

    /**
     * 生成随机的clientId
     *
     * @return ClientId
     */
    private int randomClientId() {
        return new Random().nextInt(90000000) + 10000000;
    }

    /**
     * 生成新的token
     *
     * @return 生成的token
     */
    private String generateNewToken() {
        String CHARACTERS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLNMOPQRSTUVWXYZ0123456789";
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder(24);
        for (int i = 0; i < 24; i++) {
            sb.append(CHARACTERS.charAt(random.nextInt(CHARACTERS.length())));
        }
        System.out.println(sb);
        return sb.toString();
    }
}
