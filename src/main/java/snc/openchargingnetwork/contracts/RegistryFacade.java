package snc.openchargingnetwork.contracts;

import io.reactivex.Flowable;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import org.web3j.abi.EventEncoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Bool;
import org.web3j.abi.datatypes.Event;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.Utf8String;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.RemoteCall;
import org.web3j.protocol.core.methods.request.EthFilter;
import org.web3j.protocol.core.methods.response.Log;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.tx.Contract;
import org.web3j.tx.TransactionManager;
import org.web3j.tx.gas.ContractGasProvider;

/**
 * <p>Auto generated code.
 * <p><strong>Do not modify!</strong>
 * <p>Please use the <a href="https://docs.web3j.io/command_line.html">web3j command line tools</a>,
 * or the org.web3j.codegen.SolidityFunctionWrapperGenerator in the 
 * <a href="https://github.com/web3j/web3j/tree/master/codegen">codegen module</a> to update.
 *
 * <p>Generated with web3j version 4.3.0.
 */
public class RegistryFacade extends Contract {
    private static final String BINARY = "0x6080604052336000806101000a81548173ffffffffffffffffffffffffffffffffffffffff021916908373ffffffffffffffffffffffffffffffffffffffff1602179055506000809054906101000a900473ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff16600073ffffffffffffffffffffffffffffffffffffffff167f8be0079c531659141344cd1fd0a4f28419497f9722a3daafe3b4186f6b6457e060405160405180910390a3611c35806100cf6000396000f3fe608060405234801561001057600080fd5b50600436106100bb576000357c0100000000000000000000000000000000000000000000000000000000900480638da5cb5b116100835780638da5cb5b1461047e5780638f32d59b146104c8578063a4be7c5f146104ea578063f1b2de361461061b578063f2fde38b146106d8576100bb565b806304f06401146100c05780633097a4e7146101f05780637098bf6d1461028a578063715018a6146103bb5780637f494c36146103c5575b600080fd5b6101ee600480360360808110156100d657600080fd5b8101908080357dffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff1916906020019092919080357cffffffffffffffffffffffffffffffffffffffffffffffffffffffffff19169060200190929190803573ffffffffffffffffffffffffffffffffffffffff1690602001909291908035906020019064010000000081111561016857600080fd5b82018360208201111561017a57600080fd5b8035906020019184600183028401116401000000008311171561019c57600080fd5b91908080601f016020809104026020016040519081016040528093929190818152602001838380828437600081840152601f19601f82011690508083019250505050505050919291929050505061071c565b005b610288600480360360a081101561020657600080fd5b8101908080357dffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff1916906020019092919080357cffffffffffffffffffffffffffffffffffffffffffffffffffffffffff19169060200190929190803560ff16906020019092919080359060200190929190803590602001909291905050506109bb565b005b6103b9600480360360c08110156102a057600080fd5b8101908080357dffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff1916906020019092919080357cffffffffffffffffffffffffffffffffffffffffffffffffffffffffff191690602001909291908035906020019064010000000081111561031257600080fd5b82018360208201111561032457600080fd5b8035906020019184600183028401116401000000008311171561034657600080fd5b91908080601f016020809104026020016040519081016040528093929190818152602001838380828437600081840152601f19601f820116905080830192505050505050509192919290803560ff1690602001909291908035906020019092919080359060200190929190505050610df4565b005b6103c36111c3565b005b61043c600480360360408110156103db57600080fd5b8101908080357dffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff1916906020019092919080357cffffffffffffffffffffffffffffffffffffffffffffffffffffffffff19169060200190929190505050611295565b604051808273ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff16815260200191505060405180910390f35b610486611366565b604051808273ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff16815260200191505060405180910390f35b6104d061138f565b604051808215151515815260200191505060405180910390f35b610619600480360360c081101561050057600080fd5b8101908080357dffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff1916906020019092919080357cffffffffffffffffffffffffffffffffffffffffffffffffffffffffff191690602001909291908035906020019064010000000081111561057257600080fd5b82018360208201111561058457600080fd5b803590602001918460018302840111640100000000831117156105a657600080fd5b91908080601f016020809104026020016040519081016040528093929190818152602001838380828437600081840152601f19601f820116905080830192505050505050509192919290803560ff16906020019092919080359060200190929190803590602001909291905050506113e6565b005b61065d6004803603602081101561063157600080fd5b81019080803573ffffffffffffffffffffffffffffffffffffffff16906020019092919050505061189b565b6040518080602001828103825283818151815260200191508051906020019080838360005b8381101561069d578082015181840152602081019050610682565b50505050905090810190601f1680156106ca5780820380516001836020036101000a031916815260200191505b509250505060405180910390f35b61071a600480360360208110156106ee57600080fd5b81019080803573ffffffffffffffffffffffffffffffffffffffff16906020019092919050505061197c565b005b61072461138f565b151561072f57600080fd5b600060016000867dffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff19167dffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff191681526020019081526020016000206000857cffffffffffffffffffffffffffffffffffffffffffffffffffffffffff19167cffffffffffffffffffffffffffffffffffffffffffffffffffffffffff1916815260200190815260200160002060009054906101000a900473ffffffffffffffffffffffffffffffffffffffff1690508260016000877dffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff19167dffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff191681526020019081526020016000206000867cffffffffffffffffffffffffffffffffffffffffffffffffffffffffff19167cffffffffffffffffffffffffffffffffffffffffffffffffffffffffff1916815260200190815260200160002060006101000a81548173ffffffffffffffffffffffffffffffffffffffff021916908373ffffffffffffffffffffffffffffffffffffffff16021790555081600260008573ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1681526020019081526020016000209080519060200190610932929190611a95565b508073ffffffffffffffffffffffffffffffffffffffff168373ffffffffffffffffffffffffffffffffffffffff161415156109b457600260008273ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff16815260200190815260200160002060006109b39190611b15565b5b5050505050565b6000858560405160200180837dffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff19167dffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff19168152600201827cffffffffffffffffffffffffffffffffffffffffffffffffffffffffff19167cffffffffffffffffffffffffffffffffffffffffffffffffffffffffff1916815260030192505050604051602081830303815290604052805190602001209050600060016040805190810160405280601c81526020017f19457468657265756d205369676e6564204d6573736167653a0a333200000000815250836040516020018083805190602001908083835b602083101515610ae35780518252602082019150602081019050602083039250610abe565b6001836020036101000a038019825116818451168082178552505050505050905001828152602001925050506040516020818303038152906040528051906020012086868660405160008152602001604052604051808581526020018460ff1660ff1681526020018381526020018281526020019450505050506020604051602081039080840390855afa158015610b7f573d6000803e3d6000fd5b5050506020604051035190508073ffffffffffffffffffffffffffffffffffffffff1660016000897dffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff19167dffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff191681526020019081526020016000206000887cffffffffffffffffffffffffffffffffffffffffffffffffffffffffff19167cffffffffffffffffffffffffffffffffffffffffffffffffffffffffff1916815260200190815260200160002060009054906101000a900473ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff16141515610cd7576040517f08c379a0000000000000000000000000000000000000000000000000000000008152600401808060200182810382526033815260200180611bb46033913960400191505060405180910390fd5b60016000887dffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff19167dffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff191681526020019081526020016000206000877cffffffffffffffffffffffffffffffffffffffffffffffffffffffffff19167cffffffffffffffffffffffffffffffffffffffffffffffffffffffffff1916815260200190815260200160002060006101000a81549073ffffffffffffffffffffffffffffffffffffffff0219169055600260008273ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1681526020019081526020016000206000610deb9190611b15565b50505050505050565b600086868660405160200180847dffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff19167dffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff19168152600201837cffffffffffffffffffffffffffffffffffffffffffffffffffffffffff19167cffffffffffffffffffffffffffffffffffffffffffffffffffffffffff1916815260030182805190602001908083835b602083101515610ebf5780518252602082019150602081019050602083039250610e9a565b6001836020036101000a0380198251168184511680821785525050505050509050019350505050604051602081830303815290604052805190602001209050600060016040805190810160405280601c81526020017f19457468657265756d205369676e6564204d6573736167653a0a333200000000815250836040516020018083805190602001908083835b602083101515610f715780518252602082019150602081019050602083039250610f4c565b6001836020036101000a038019825116818451168082178552505050505050905001828152602001925050506040516020818303038152906040528051906020012086868660405160008152602001604052604051808581526020018460ff1660ff1681526020018381526020018281526020019450505050506020604051602081039080840390855afa15801561100d573d6000803e3d6000fd5b5050506020604051035190508073ffffffffffffffffffffffffffffffffffffffff16600160008a7dffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff19167dffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff191681526020019081526020016000206000897cffffffffffffffffffffffffffffffffffffffffffffffffffffffffff19167cffffffffffffffffffffffffffffffffffffffffffffffffffffffffff1916815260200190815260200160002060009054906101000a900473ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff16141515611165576040517f08c379a0000000000000000000000000000000000000000000000000000000008152600401808060200182810382526031815260200180611b836031913960400191505060405180910390fd5b85600260008373ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff16815260200190815260200160002090805190602001906111b8929190611a95565b505050505050505050565b6111cb61138f565b15156111d657600080fd5b600073ffffffffffffffffffffffffffffffffffffffff166000809054906101000a900473ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff167f8be0079c531659141344cd1fd0a4f28419497f9722a3daafe3b4186f6b6457e060405160405180910390a360008060006101000a81548173ffffffffffffffffffffffffffffffffffffffff021916908373ffffffffffffffffffffffffffffffffffffffff160217905550565b600060016000847dffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff19167dffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff191681526020019081526020016000206000837cffffffffffffffffffffffffffffffffffffffffffffffffffffffffff19167cffffffffffffffffffffffffffffffffffffffffffffffffffffffffff1916815260200190815260200160002060009054906101000a900473ffffffffffffffffffffffffffffffffffffffff16905092915050565b60008060009054906101000a900473ffffffffffffffffffffffffffffffffffffffff16905090565b60008060009054906101000a900473ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff163373ffffffffffffffffffffffffffffffffffffffff1614905090565b600073ffffffffffffffffffffffffffffffffffffffff1660016000887dffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff19167dffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff191681526020019081526020016000206000877cffffffffffffffffffffffffffffffffffffffffffffffffffffffffff19167cffffffffffffffffffffffffffffffffffffffffffffffffffffffffff1916815260200190815260200160002060009054906101000a900473ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff16141515611533576040517f08c379a0000000000000000000000000000000000000000000000000000000008152600401808060200182810382526023815260200180611be76023913960400191505060405180910390fd5b600086868660405160200180847dffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff19167dffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff19168152600201837cffffffffffffffffffffffffffffffffffffffffffffffffffffffffff19167cffffffffffffffffffffffffffffffffffffffffffffffffffffffffff1916815260030182805190602001908083835b6020831015156115fe57805182526020820191506020810190506020830392506115d9565b6001836020036101000a0380198251168184511680821785525050505050509050019350505050604051602081830303815290604052805190602001209050600060016040805190810160405280601c81526020017f19457468657265756d205369676e6564204d6573736167653a0a333200000000815250836040516020018083805190602001908083835b6020831015156116b0578051825260208201915060208101905060208303925061168b565b6001836020036101000a038019825116818451168082178552505050505050905001828152602001925050506040516020818303038152906040528051906020012086868660405160008152602001604052604051808581526020018460ff1660ff1681526020018381526020018281526020019450505050506020604051602081039080840390855afa15801561174c573d6000803e3d6000fd5b50505060206040510351905080600160008a7dffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff19167dffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff191681526020019081526020016000206000897cffffffffffffffffffffffffffffffffffffffffffffffffffffffffff19167cffffffffffffffffffffffffffffffffffffffffffffffffffffffffff1916815260200190815260200160002060006101000a81548173ffffffffffffffffffffffffffffffffffffffff021916908373ffffffffffffffffffffffffffffffffffffffff16021790555085600260008373ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1681526020019081526020016000209080519060200190611890929190611a95565b505050505050505050565b6060600260008373ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1681526020019081526020016000208054600181600116156101000203166002900480601f0160208091040260200160405190810160405280929190818152602001828054600181600116156101000203166002900480156119705780601f1061194557610100808354040283529160200191611970565b820191906000526020600020905b81548152906001019060200180831161195357829003601f168201915b50505050509050919050565b61198461138f565b151561198f57600080fd5b6119988161199b565b50565b600073ffffffffffffffffffffffffffffffffffffffff168173ffffffffffffffffffffffffffffffffffffffff16141515156119d757600080fd5b8073ffffffffffffffffffffffffffffffffffffffff166000809054906101000a900473ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff167f8be0079c531659141344cd1fd0a4f28419497f9722a3daafe3b4186f6b6457e060405160405180910390a3806000806101000a81548173ffffffffffffffffffffffffffffffffffffffff021916908373ffffffffffffffffffffffffffffffffffffffff16021790555050565b828054600181600116156101000203166002900490600052602060002090601f016020900481019282601f10611ad657805160ff1916838001178555611b04565b82800160010185558215611b04579182015b82811115611b03578251825591602001919060010190611ae8565b5b509050611b119190611b5d565b5090565b50805460018160011615610100020316600290046000825580601f10611b3b5750611b5a565b601f016020900490600052602060002090810190611b599190611b5d565b5b50565b611b7f91905b80821115611b7b576000816000905550600101611b63565b5090565b9056fe556e617574686f72697a656420746f20757064617465207468697320656e74727920696e20746865207265676973747279556e617574686f72697a656420746f2072656d6f7665207468697320656e7472792066726f6d20746865207265676973747279506172747920494420616c72656164792065786973747320696e207265676973747279a165627a7a72305820b70a3229e0bdfff93ac7dc04e0fa020c8a5a7c8bc06c5177d0da95a5151c635b0029";

    public static final String FUNC_RENOUNCEOWNERSHIP = "renounceOwnership";

    public static final String FUNC_OWNER = "owner";

    public static final String FUNC_ISOWNER = "isOwner";

    public static final String FUNC_TRANSFEROWNERSHIP = "transferOwnership";

    public static final String FUNC_ADMINOVERWRITE = "adminOverwrite";

    public static final String FUNC_REGISTER = "register";

    public static final String FUNC_DEREGISTER = "deregister";

    public static final String FUNC_UPDATEBROKERURL = "updateBrokerURL";

    public static final String FUNC_ADDRESSOF = "addressOf";

    public static final String FUNC_BROKEROF = "brokerOf";

    public static final Event OWNERSHIPTRANSFERRED_EVENT = new Event("OwnershipTransferred", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Address>(true) {}, new TypeReference<Address>(true) {}));
    ;

    protected static final HashMap<String, String> _addresses;

    static {
        _addresses = new HashMap<String, String>();
        _addresses.put("9", "0x3ba7c2578B59e0e1CcfeE9A20D92F043C0e0b3e6");
    }

    @Deprecated
    protected RegistryFacade(String contractAddress, Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    protected RegistryFacade(String contractAddress, Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider) {
        super(BINARY, contractAddress, web3j, credentials, contractGasProvider);
    }

    @Deprecated
    protected RegistryFacade(String contractAddress, Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    protected RegistryFacade(String contractAddress, Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        super(BINARY, contractAddress, web3j, transactionManager, contractGasProvider);
    }

    public RemoteCall<TransactionReceipt> renounceOwnership() {
        final Function function = new Function(
                FUNC_RENOUNCEOWNERSHIP, 
                Arrays.<Type>asList(), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteCall<String> owner() {
        final Function function = new Function(FUNC_OWNER, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}));
        return executeRemoteCallSingleValueReturn(function, String.class);
    }

    public RemoteCall<Boolean> isOwner() {
        final Function function = new Function(FUNC_ISOWNER, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Bool>() {}));
        return executeRemoteCallSingleValueReturn(function, Boolean.class);
    }

    public RemoteCall<TransactionReceipt> transferOwnership(String newOwner) {
        final Function function = new Function(
                FUNC_TRANSFEROWNERSHIP, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(newOwner)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public List<OwnershipTransferredEventResponse> getOwnershipTransferredEvents(TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = extractEventParametersWithLog(OWNERSHIPTRANSFERRED_EVENT, transactionReceipt);
        ArrayList<OwnershipTransferredEventResponse> responses = new ArrayList<OwnershipTransferredEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            OwnershipTransferredEventResponse typedResponse = new OwnershipTransferredEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.previousOwner = (String) eventValues.getIndexedValues().get(0).getValue();
            typedResponse.newOwner = (String) eventValues.getIndexedValues().get(1).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public Flowable<OwnershipTransferredEventResponse> ownershipTransferredEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(new io.reactivex.functions.Function<Log, OwnershipTransferredEventResponse>() {
            @Override
            public OwnershipTransferredEventResponse apply(Log log) {
                Contract.EventValuesWithLog eventValues = extractEventParametersWithLog(OWNERSHIPTRANSFERRED_EVENT, log);
                OwnershipTransferredEventResponse typedResponse = new OwnershipTransferredEventResponse();
                typedResponse.log = log;
                typedResponse.previousOwner = (String) eventValues.getIndexedValues().get(0).getValue();
                typedResponse.newOwner = (String) eventValues.getIndexedValues().get(1).getValue();
                return typedResponse;
            }
        });
    }

    public Flowable<OwnershipTransferredEventResponse> ownershipTransferredEventFlowable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(OWNERSHIPTRANSFERRED_EVENT));
        return ownershipTransferredEventFlowable(filter);
    }

    public RemoteCall<TransactionReceipt> adminOverwrite(byte[] countryCode, byte[] partyID, String newIdentity, String newBrokerURL) {
        final Function function = new Function(
                FUNC_ADMINOVERWRITE, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Bytes2(countryCode), 
                new org.web3j.abi.datatypes.generated.Bytes3(partyID), 
                new org.web3j.abi.datatypes.Address(newIdentity), 
                new org.web3j.abi.datatypes.Utf8String(newBrokerURL)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteCall<TransactionReceipt> register(byte[] countryCode, byte[] partyID, String brokerURL, BigInteger v, byte[] r, byte[] s) {
        final Function function = new Function(
                FUNC_REGISTER, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Bytes2(countryCode), 
                new org.web3j.abi.datatypes.generated.Bytes3(partyID), 
                new org.web3j.abi.datatypes.Utf8String(brokerURL), 
                new org.web3j.abi.datatypes.generated.Uint8(v), 
                new org.web3j.abi.datatypes.generated.Bytes32(r), 
                new org.web3j.abi.datatypes.generated.Bytes32(s)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteCall<TransactionReceipt> deregister(byte[] countryCode, byte[] partyID, BigInteger v, byte[] r, byte[] s) {
        final Function function = new Function(
                FUNC_DEREGISTER, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Bytes2(countryCode), 
                new org.web3j.abi.datatypes.generated.Bytes3(partyID), 
                new org.web3j.abi.datatypes.generated.Uint8(v), 
                new org.web3j.abi.datatypes.generated.Bytes32(r), 
                new org.web3j.abi.datatypes.generated.Bytes32(s)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteCall<TransactionReceipt> updateBrokerURL(byte[] countryCode, byte[] partyID, String brokerURL, BigInteger v, byte[] r, byte[] s) {
        final Function function = new Function(
                FUNC_UPDATEBROKERURL, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Bytes2(countryCode), 
                new org.web3j.abi.datatypes.generated.Bytes3(partyID), 
                new org.web3j.abi.datatypes.Utf8String(brokerURL), 
                new org.web3j.abi.datatypes.generated.Uint8(v), 
                new org.web3j.abi.datatypes.generated.Bytes32(r), 
                new org.web3j.abi.datatypes.generated.Bytes32(s)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteCall<String> addressOf(byte[] countryCode, byte[] partyID) {
        final Function function = new Function(FUNC_ADDRESSOF, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Bytes2(countryCode), 
                new org.web3j.abi.datatypes.generated.Bytes3(partyID)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}));
        return executeRemoteCallSingleValueReturn(function, String.class);
    }

    public RemoteCall<String> brokerOf(String partyAddress) {
        final Function function = new Function(FUNC_BROKEROF, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(partyAddress)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Utf8String>() {}));
        return executeRemoteCallSingleValueReturn(function, String.class);
    }

    @Deprecated
    public static RegistryFacade load(String contractAddress, Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        return new RegistryFacade(contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    @Deprecated
    public static RegistryFacade load(String contractAddress, Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        return new RegistryFacade(contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    public static RegistryFacade load(String contractAddress, Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider) {
        return new RegistryFacade(contractAddress, web3j, credentials, contractGasProvider);
    }

    public static RegistryFacade load(String contractAddress, Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        return new RegistryFacade(contractAddress, web3j, transactionManager, contractGasProvider);
    }

    public static RemoteCall<RegistryFacade> deploy(Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider) {
        return deployRemoteCall(RegistryFacade.class, web3j, credentials, contractGasProvider, BINARY, "");
    }

    @Deprecated
    public static RemoteCall<RegistryFacade> deploy(Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        return deployRemoteCall(RegistryFacade.class, web3j, credentials, gasPrice, gasLimit, BINARY, "");
    }

    public static RemoteCall<RegistryFacade> deploy(Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        return deployRemoteCall(RegistryFacade.class, web3j, transactionManager, contractGasProvider, BINARY, "");
    }

    @Deprecated
    public static RemoteCall<RegistryFacade> deploy(Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        return deployRemoteCall(RegistryFacade.class, web3j, transactionManager, gasPrice, gasLimit, BINARY, "");
    }

    protected String getStaticDeployedAddress(String networkId) {
        return _addresses.get(networkId);
    }

    public static String getPreviouslyDeployedAddress(String networkId) {
        return _addresses.get(networkId);
    }

    public static class OwnershipTransferredEventResponse {
        public Log log;

        public String previousOwner;

        public String newOwner;
    }
}
