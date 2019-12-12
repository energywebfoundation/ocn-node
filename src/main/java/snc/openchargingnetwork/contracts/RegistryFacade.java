package snc.openchargingnetwork.contracts;

import io.reactivex.Flowable;
import io.reactivex.functions.Function;
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
    private static final String BINARY = "0x6080604052336000806101000a81548173ffffffffffffffffffffffffffffffffffffffff021916908373ffffffffffffffffffffffffffffffffffffffff1602179055506000809054906101000a900473ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff16600073ffffffffffffffffffffffffffffffffffffffff167f8be0079c531659141344cd1fd0a4f28419497f9722a3daafe3b4186f6b6457e060405160405180910390a36121ac806100cf6000396000f3fe608060405234801561001057600080fd5b50600436106100a95760003560e01c8063a06c124d11610071578063a06c124d1461030e578063cd79f6081461045f578063d62e24b0146105b0578063e995527214610669578063f22155fe14610722578063f2fde38b14610814576100a9565b80633097a4e7146100ae5780636e5ffcd414610148578063715018a6146102985780638da5cb5b146102a25780638f32d59b146102ec575b600080fd5b610146600480360360a08110156100c457600080fd5b8101908080357dffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff1916906020019092919080357cffffffffffffffffffffffffffffffffffffffffffffffffffffffffff19169060200190929190803560ff1690602001909291908035906020019092919080359060200190929190505050610858565b005b610296600480360360a081101561015e57600080fd5b8101908080357dffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff1916906020019092919080357cffffffffffffffffffffffffffffffffffffffffffffffffffffffffff19169060200190929190803573ffffffffffffffffffffffffffffffffffffffff169060200190929190803590602001906401000000008111156101f057600080fd5b82018360208201111561020257600080fd5b8035906020019184600183028401116401000000008311171561022457600080fd5b91908080601f016020809104026020016040519081016040528093929190818152602001838380828437600081840152601f19601f820116905080830192505050505050509192919290803573ffffffffffffffffffffffffffffffffffffffff169060200190929190505050610cbb565b005b6102a0611002565b005b6102aa6110d2565b604051808273ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff16815260200191505060405180910390f35b6102f46110fb565b604051808215151515815260200191505060405180910390f35b61045d600480360360e081101561032457600080fd5b8101908080357dffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff1916906020019092919080357cffffffffffffffffffffffffffffffffffffffffffffffffffffffffff191690602001909291908035906020019064010000000081111561039657600080fd5b8201836020820111156103a857600080fd5b803590602001918460018302840111640100000000831117156103ca57600080fd5b91908080601f016020809104026020016040519081016040528093929190818152602001838380828437600081840152601f19601f820116905080830192505050505050509192919290803573ffffffffffffffffffffffffffffffffffffffff169060200190929190803560ff1690602001909291908035906020019092919080359060200190929190505050611152565b005b6105ae600480360360e081101561047557600080fd5b8101908080357dffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff1916906020019092919080357cffffffffffffffffffffffffffffffffffffffffffffffffffffffffff19169060200190929190803590602001906401000000008111156104e757600080fd5b8201836020820111156104f957600080fd5b8035906020019184600183028401116401000000008311171561051b57600080fd5b91908080601f016020809104026020016040519081016040528093929190818152602001838380828437600081840152601f19601f820116905080830192505050505050509192919290803573ffffffffffffffffffffffffffffffffffffffff169060200190929190803560ff16906020019092919080359060200190929190803590602001909291905050506116b6565b005b610627600480360360408110156105c657600080fd5b8101908080357dffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff1916906020019092919080357cffffffffffffffffffffffffffffffffffffffffffffffffffffffffff19169060200190929190505050611b34565b604051808273ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff16815260200191505060405180910390f35b6106e06004803603604081101561067f57600080fd5b8101908080357dffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff1916906020019092919080357cffffffffffffffffffffffffffffffffffffffffffffffffffffffffff19169060200190929190505050611c05565b604051808273ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff16815260200191505060405180910390f35b6107996004803603604081101561073857600080fd5b8101908080357dffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff1916906020019092919080357cffffffffffffffffffffffffffffffffffffffffffffffffffffffffff19169060200190929190505050611d3d565b6040518080602001828103825283818151815260200191508051906020019080838360005b838110156107d95780820151818401526020810190506107be565b50505050905090810190601f1680156108065780820380516001836020036101000a031916815260200191505b509250505060405180910390f35b6108566004803603602081101561082a57600080fd5b81019080803573ffffffffffffffffffffffffffffffffffffffff169060200190929190505050611eee565b005b6000858560405160200180837dffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff19167dffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff19168152600201827cffffffffffffffffffffffffffffffffffffffffffffffffffffffffff19167cffffffffffffffffffffffffffffffffffffffffffffffffffffffffff1916815260030192505050604051602081830303815290604052805190602001209050600060016040518060400160405280601c81526020017f19457468657265756d205369676e6564204d6573736167653a0a333200000000815250836040516020018083805190602001908083835b6020831061097e578051825260208201915060208101905060208303925061095b565b6001836020036101000a038019825116818451168082178552505050505050905001828152602001925050506040516020818303038152906040528051906020012086868660405160008152602001604052604051808581526020018460ff1660ff1681526020018381526020018281526020019450505050506020604051602081039080840390855afa158015610a1a573d6000803e3d6000fd5b5050506020604051035190508073ffffffffffffffffffffffffffffffffffffffff1660016000897dffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff19167dffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff191681526020019081526020016000206000887cffffffffffffffffffffffffffffffffffffffffffffffffffffffffff19167cffffffffffffffffffffffffffffffffffffffffffffffffffffffffff1916815260200190815260200160002060009054906101000a900473ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1614610b70576040517f08c379a00000000000000000000000000000000000000000000000000000000081526004018080602001828103825260338152602001806121226033913960400191505060405180910390fd5b60016000887dffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff19167dffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff191681526020019081526020016000206000877cffffffffffffffffffffffffffffffffffffffffffffffffffffffffff19167cffffffffffffffffffffffffffffffffffffffffffffffffffffffffff1916815260200190815260200160002060006101000a81549073ffffffffffffffffffffffffffffffffffffffff0219169055600260008273ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff16815260200190815260200160002060008082016000610c899190612003565b6001820160006101000a81549073ffffffffffffffffffffffffffffffffffffffff0219169055505050505050505050565b610cc36110fb565b610ccc57600080fd5b600060016000877dffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff19167dffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff191681526020019081526020016000206000867cffffffffffffffffffffffffffffffffffffffffffffffffffffffffff19167cffffffffffffffffffffffffffffffffffffffffffffffffffffffffff1916815260200190815260200160002060009054906101000a900473ffffffffffffffffffffffffffffffffffffffff1690508360016000887dffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff19167dffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff191681526020019081526020016000206000877cffffffffffffffffffffffffffffffffffffffffffffffffffffffffff19167cffffffffffffffffffffffffffffffffffffffffffffffffffffffffff1916815260200190815260200160002060006101000a81548173ffffffffffffffffffffffffffffffffffffffff021916908373ffffffffffffffffffffffffffffffffffffffff16021790555060405180604001604052808481526020018373ffffffffffffffffffffffffffffffffffffffff16815250600260008673ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1681526020019081526020016000206000820151816000019080519060200190610f0292919061204b565b5060208201518160010160006101000a81548173ffffffffffffffffffffffffffffffffffffffff021916908373ffffffffffffffffffffffffffffffffffffffff1602179055509050508073ffffffffffffffffffffffffffffffffffffffff168473ffffffffffffffffffffffffffffffffffffffff1614610ffa57600260008273ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff16815260200190815260200160002060008082016000610fd09190612003565b6001820160006101000a81549073ffffffffffffffffffffffffffffffffffffffff021916905550505b505050505050565b61100a6110fb565b61101357600080fd5b600073ffffffffffffffffffffffffffffffffffffffff166000809054906101000a900473ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff167f8be0079c531659141344cd1fd0a4f28419497f9722a3daafe3b4186f6b6457e060405160405180910390a360008060006101000a81548173ffffffffffffffffffffffffffffffffffffffff021916908373ffffffffffffffffffffffffffffffffffffffff160217905550565b60008060009054906101000a900473ffffffffffffffffffffffffffffffffffffffff16905090565b60008060009054906101000a900473ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff163373ffffffffffffffffffffffffffffffffffffffff1614905090565b600073ffffffffffffffffffffffffffffffffffffffff1660016000897dffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff19167dffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff191681526020019081526020016000206000887cffffffffffffffffffffffffffffffffffffffffffffffffffffffffff19167cffffffffffffffffffffffffffffffffffffffffffffffffffffffffff1916815260200190815260200160002060009054906101000a900473ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff161461129d576040517f08c379a00000000000000000000000000000000000000000000000000000000081526004018080602001828103825260238152602001806121556023913960400191505060405180910390fd5b60008787878760405160200180857dffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff19167dffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff19168152600201847cffffffffffffffffffffffffffffffffffffffffffffffffffffffffff19167cffffffffffffffffffffffffffffffffffffffffffffffffffffffffff1916815260030183805190602001908083835b602083106113675780518252602082019150602081019050602083039250611344565b6001836020036101000a0380198251168184511680821785525050505050509050018273ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1660601b8152601401945050505050604051602081830303815290604052805190602001209050600060016040518060400160405280601c81526020017f19457468657265756d205369676e6564204d6573736167653a0a333200000000815250836040516020018083805190602001908083835b6020831061144d578051825260208201915060208101905060208303925061142a565b6001836020036101000a038019825116818451168082178552505050505050905001828152602001925050506040516020818303038152906040528051906020012086868660405160008152602001604052604051808581526020018460ff1660ff1681526020018381526020018281526020019450505050506020604051602081039080840390855afa1580156114e9573d6000803e3d6000fd5b50505060206040510351905080600160008b7dffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff19167dffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff1916815260200190815260200160002060008a7cffffffffffffffffffffffffffffffffffffffffffffffffffffffffff19167cffffffffffffffffffffffffffffffffffffffffffffffffffffffffff1916815260200190815260200160002060006101000a81548173ffffffffffffffffffffffffffffffffffffffff021916908373ffffffffffffffffffffffffffffffffffffffff16021790555060405180604001604052808881526020018773ffffffffffffffffffffffffffffffffffffffff16815250600260008373ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff168152602001908152602001600020600082015181600001908051906020019061166092919061204b565b5060208201518160010160006101000a81548173ffffffffffffffffffffffffffffffffffffffff021916908373ffffffffffffffffffffffffffffffffffffffff160217905550905050505050505050505050565b60008787878760405160200180857dffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff19167dffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff19168152600201847cffffffffffffffffffffffffffffffffffffffffffffffffffffffffff19167cffffffffffffffffffffffffffffffffffffffffffffffffffffffffff1916815260030183805190602001908083835b60208310611780578051825260208201915060208101905060208303925061175d565b6001836020036101000a0380198251168184511680821785525050505050509050018273ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1660601b8152601401945050505050604051602081830303815290604052805190602001209050600060016040518060400160405280601c81526020017f19457468657265756d205369676e6564204d6573736167653a0a333200000000815250836040516020018083805190602001908083835b602083106118665780518252602082019150602081019050602083039250611843565b6001836020036101000a038019825116818451168082178552505050505050905001828152602001925050506040516020818303038152906040528051906020012086868660405160008152602001604052604051808581526020018460ff1660ff1681526020018381526020018281526020019450505050506020604051602081039080840390855afa158015611902573d6000803e3d6000fd5b5050506020604051035190508073ffffffffffffffffffffffffffffffffffffffff16600160008b7dffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff19167dffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff1916815260200190815260200160002060008a7cffffffffffffffffffffffffffffffffffffffffffffffffffffffffff19167cffffffffffffffffffffffffffffffffffffffffffffffffffffffffff1916815260200190815260200160002060009054906101000a900473ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1614611a58576040517f08c379a00000000000000000000000000000000000000000000000000000000081526004018080602001828103825260318152602001806120f16031913960400191505060405180910390fd5b60405180604001604052808881526020018773ffffffffffffffffffffffffffffffffffffffff16815250600260008373ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1681526020019081526020016000206000820151816000019080519060200190611ade92919061204b565b5060208201518160010160006101000a81548173ffffffffffffffffffffffffffffffffffffffff021916908373ffffffffffffffffffffffffffffffffffffffff160217905550905050505050505050505050565b600060016000847dffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff19167dffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff191681526020019081526020016000206000837cffffffffffffffffffffffffffffffffffffffffffffffffffffffffff19167cffffffffffffffffffffffffffffffffffffffffffffffffffffffffff1916815260200190815260200160002060009054906101000a900473ffffffffffffffffffffffffffffffffffffffff16905092915050565b60008060016000857dffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff19167dffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff191681526020019081526020016000206000847cffffffffffffffffffffffffffffffffffffffffffffffffffffffffff19167cffffffffffffffffffffffffffffffffffffffffffffffffffffffffff1916815260200190815260200160002060009054906101000a900473ffffffffffffffffffffffffffffffffffffffff169050600260008273ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff16815260200190815260200160002060010160009054906101000a900473ffffffffffffffffffffffffffffffffffffffff1691505092915050565b6060600060016000857dffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff19167dffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff191681526020019081526020016000206000847cffffffffffffffffffffffffffffffffffffffffffffffffffffffffff19167cffffffffffffffffffffffffffffffffffffffffffffffffffffffffff1916815260200190815260200160002060009054906101000a900473ffffffffffffffffffffffffffffffffffffffff169050600260008273ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1681526020019081526020016000206000018054600181600116156101000203166002900480601f016020809104026020016040519081016040528092919081815260200182805460018160011615610100020316600290048015611ee05780601f10611eb557610100808354040283529160200191611ee0565b820191906000526020600020905b815481529060010190602001808311611ec357829003601f168201915b505050505091505092915050565b611ef66110fb565b611eff57600080fd5b611f0881611f0b565b50565b600073ffffffffffffffffffffffffffffffffffffffff168173ffffffffffffffffffffffffffffffffffffffff161415611f4557600080fd5b8073ffffffffffffffffffffffffffffffffffffffff166000809054906101000a900473ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff167f8be0079c531659141344cd1fd0a4f28419497f9722a3daafe3b4186f6b6457e060405160405180910390a3806000806101000a81548173ffffffffffffffffffffffffffffffffffffffff021916908373ffffffffffffffffffffffffffffffffffffffff16021790555050565b50805460018160011615610100020316600290046000825580601f106120295750612048565b601f01602090049060005260206000209081019061204791906120cb565b5b50565b828054600181600116156101000203166002900490600052602060002090601f016020900481019282601f1061208c57805160ff19168380011785556120ba565b828001600101855582156120ba579182015b828111156120b957825182559160200191906001019061209e565b5b5090506120c791906120cb565b5090565b6120ed91905b808211156120e95760008160009055506001016120d1565b5090565b9056fe556e617574686f72697a656420746f20757064617465207468697320656e74727920696e20746865207265676973747279556e617574686f72697a656420746f2072656d6f7665207468697320656e7472792066726f6d20746865207265676973747279506172747920494420616c72656164792065786973747320696e207265676973747279a265627a7a72315820640f7a4065f48461e404865a7c11977f84592747d0291e1dd22d0ad79feeeed264736f6c634300050e0032";

    public static final String FUNC_ISOWNER = "isOwner";

    public static final String FUNC_OWNER = "owner";

    public static final String FUNC_RENOUNCEOWNERSHIP = "renounceOwnership";

    public static final String FUNC_TRANSFEROWNERSHIP = "transferOwnership";

    public static final String FUNC_ADMINOVERWRITE = "adminOverwrite";

    public static final String FUNC_REGISTER = "register";

    public static final String FUNC_DEREGISTER = "deregister";

    public static final String FUNC_UPDATENODEINFO = "updateNodeInfo";

    public static final String FUNC_PARTYADDRESSOF = "partyAddressOf";

    public static final String FUNC_NODEURLOF = "nodeURLOf";

    public static final String FUNC_NODEADDRESSOF = "nodeAddressOf";

    public static final Event OWNERSHIPTRANSFERRED_EVENT = new Event("OwnershipTransferred", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Address>(true) {}, new TypeReference<Address>(true) {}));
    ;

    protected static final HashMap<String, String> _addresses;

    static {
        _addresses = new HashMap<String, String>();
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
        return web3j.ethLogFlowable(filter).map(new Function<Log, OwnershipTransferredEventResponse>() {
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

    public RemoteCall<Boolean> isOwner() {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(FUNC_ISOWNER, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Bool>() {}));
        return executeRemoteCallSingleValueReturn(function, Boolean.class);
    }

    public RemoteCall<String> owner() {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(FUNC_OWNER, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}));
        return executeRemoteCallSingleValueReturn(function, String.class);
    }

    public RemoteCall<TransactionReceipt> renounceOwnership() {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(
                FUNC_RENOUNCEOWNERSHIP, 
                Arrays.<Type>asList(), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteCall<TransactionReceipt> transferOwnership(String newOwner) {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(
                FUNC_TRANSFEROWNERSHIP, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(newOwner)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteCall<TransactionReceipt> adminOverwrite(byte[] countryCode, byte[] partyID, String newRoleAddress, String newNodeURL, String newNodeAddress) {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(
                FUNC_ADMINOVERWRITE, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Bytes2(countryCode), 
                new org.web3j.abi.datatypes.generated.Bytes3(partyID), 
                new org.web3j.abi.datatypes.Address(newRoleAddress), 
                new org.web3j.abi.datatypes.Utf8String(newNodeURL), 
                new org.web3j.abi.datatypes.Address(newNodeAddress)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteCall<TransactionReceipt> register(byte[] countryCode, byte[] partyID, String nodeURL, String nodeAddress, BigInteger v, byte[] r, byte[] s) {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(
                FUNC_REGISTER, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Bytes2(countryCode), 
                new org.web3j.abi.datatypes.generated.Bytes3(partyID), 
                new org.web3j.abi.datatypes.Utf8String(nodeURL), 
                new org.web3j.abi.datatypes.Address(nodeAddress), 
                new org.web3j.abi.datatypes.generated.Uint8(v), 
                new org.web3j.abi.datatypes.generated.Bytes32(r), 
                new org.web3j.abi.datatypes.generated.Bytes32(s)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteCall<TransactionReceipt> deregister(byte[] countryCode, byte[] partyID, BigInteger v, byte[] r, byte[] s) {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(
                FUNC_DEREGISTER, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Bytes2(countryCode), 
                new org.web3j.abi.datatypes.generated.Bytes3(partyID), 
                new org.web3j.abi.datatypes.generated.Uint8(v), 
                new org.web3j.abi.datatypes.generated.Bytes32(r), 
                new org.web3j.abi.datatypes.generated.Bytes32(s)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteCall<TransactionReceipt> updateNodeInfo(byte[] countryCode, byte[] partyID, String newNodeURL, String newNodeAddress, BigInteger v, byte[] r, byte[] s) {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(
                FUNC_UPDATENODEINFO, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Bytes2(countryCode), 
                new org.web3j.abi.datatypes.generated.Bytes3(partyID), 
                new org.web3j.abi.datatypes.Utf8String(newNodeURL), 
                new org.web3j.abi.datatypes.Address(newNodeAddress), 
                new org.web3j.abi.datatypes.generated.Uint8(v), 
                new org.web3j.abi.datatypes.generated.Bytes32(r), 
                new org.web3j.abi.datatypes.generated.Bytes32(s)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteCall<String> partyAddressOf(byte[] countryCode, byte[] partyID) {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(FUNC_PARTYADDRESSOF, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Bytes2(countryCode), 
                new org.web3j.abi.datatypes.generated.Bytes3(partyID)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}));
        return executeRemoteCallSingleValueReturn(function, String.class);
    }

    public RemoteCall<String> nodeURLOf(byte[] countryCode, byte[] partyID) {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(FUNC_NODEURLOF, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Bytes2(countryCode), 
                new org.web3j.abi.datatypes.generated.Bytes3(partyID)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Utf8String>() {}));
        return executeRemoteCallSingleValueReturn(function, String.class);
    }

    public RemoteCall<String> nodeAddressOf(byte[] countryCode, byte[] partyID) {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(FUNC_NODEADDRESSOF, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Bytes2(countryCode), 
                new org.web3j.abi.datatypes.generated.Bytes3(partyID)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}));
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
