package org.example.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.example.entity.dto.Client;
import org.example.entity.vo.request.*;
import org.example.entity.vo.response.*;

import java.util.List;

public interface ClientService extends IService<Client> {

    boolean verifyAndRegister(String token);

    Client findClientById(int id);

    Client findClientByToken(String token);

    String registerToken();

    void updateClientDetails(ClientDetailVO vo, Client client);

    void updateRuntimeDetails(RuntimeDetailVO vo, Client client);

    List<ClientPreviewVO> listAllClient();

    // 客户端修改名称
    void renameClient(RenameClientVO vo);

    // 客户端节点重命名
    void renameNode(RenameNodeVO vo);

    // 用于在卡片内展示客户端详细信息
    ClientDetailsVO clientDetails(int clientId);

    // 获取近一个小时的服务器数据
    RuntimeHistoryVO clientRuntimeDetailsHistory(int clientId);

    // 获取当前的服务器数据
    RuntimeDetailVO clientRuntimeDetailsNow(int clientId);

    // 删除指定的客户端
    void deleteClient(int clientId);

    // 新建子用户分配主机时展示主机列表
    List<ClientSimpleVO> listSimpleList();

    // 保存ssh链接信息, 下次无需再次输入
    void saveClientSshConnection(SshConnectionVO vo);

    // 获取ssh连接信息
    SshSettingVO sshSettings(int clientId);
}
