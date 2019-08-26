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
    private static final String BINARY = "0x6080604052336000806101000a81548173ffffffffffffffffffffffffffffffffffffffff021916908373ffffffffffffffffffffffffffffffffffffffff1602179055506000809054906101000a900473ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff16600073ffffffffffffffffffffffffffffffffffffffff167f8be0079c531659141344cd1fd0a4f28419497f9722a3daafe3b4186f6b6457e060405160405180910390a361205d806100cf6000396000f3fe608060405234801561001057600080fd5b50600436106100bb576000357c010000000000000000000000000000000000000000000000000000000090048063715018a611610083578063715018a6146105a65780638da5cb5b146105b05780638f32d59b146105fa578063a06c124d1461061c578063f2fde38b1461076d576100bb565b8063013ead6b146100c057806320bb4f18146101b25780633097a4e7146103035780633e5ac4091461039d5780636e5ffcd414610456575b600080fd5b610137600480360360408110156100d657600080fd5b8101908080357dffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff1916906020019092919080357cffffffffffffffffffffffffffffffffffffffffffffffffffffffffff191690602001909291905050506107b1565b6040518080602001828103825283818151815260200191508051906020019080838360005b8381101561017757808201518184015260208101905061015c565b50505050905090810190601f1680156101a45780820380516001836020036101000a031916815260200191505b509250505060405180910390f35b610301600480360360e08110156101c857600080fd5b8101908080357dffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff1916906020019092919080357cffffffffffffffffffffffffffffffffffffffffffffffffffffffffff191690602001909291908035906020019064010000000081111561023a57600080fd5b82018360208201111561024c57600080fd5b8035906020019184600183028401116401000000008311171561026e57600080fd5b91908080601f016020809104026020016040519081016040528093929190818152602001838380828437600081840152601f19601f820116905080830192505050505050509192919290803573ffffffffffffffffffffffffffffffffffffffff169060200190929190803560ff1690602001909291908035906020019092919080359060200190929190505050610962565b005b61039b600480360360a081101561031957600080fd5b8101908080357dffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff1916906020019092919080357cffffffffffffffffffffffffffffffffffffffffffffffffffffffffff19169060200190929190803560ff1690602001909291908035906020019092919080359060200190929190505050610df2565b005b610414600480360360408110156103b357600080fd5b8101908080357dffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff1916906020019092919080357cffffffffffffffffffffffffffffffffffffffffffffffffffffffffff19169060200190929190505050611259565b604051808273ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff16815260200191505060405180910390f35b6105a4600480360360a081101561046c57600080fd5b8101908080357dffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff1916906020019092919080357cffffffffffffffffffffffffffffffffffffffffffffffffffffffffff19169060200190929190803573ffffffffffffffffffffffffffffffffffffffff169060200190929190803590602001906401000000008111156104fe57600080fd5b82018360208201111561051057600080fd5b8035906020019184600183028401116401000000008311171561053257600080fd5b91908080601f016020809104026020016040519081016040528093929190818152602001838380828437600081840152601f19601f820116905080830192505050505050509192919290803573ffffffffffffffffffffffffffffffffffffffff169060200190929190505050611391565b005b6105ae6116dc565b005b6105b86117ae565b604051808273ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff16815260200191505060405180910390f35b6106026117d7565b604051808215151515815260200191505060405180910390f35b61076b600480360360e081101561063257600080fd5b8101908080357dffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff1916906020019092919080357cffffffffffffffffffffffffffffffffffffffffffffffffffffffffff19169060200190929190803590602001906401000000008111156106a457600080fd5b8201836020820111156106b657600080fd5b803590602001918460018302840111640100000000831117156106d857600080fd5b91908080601f016020809104026020016040519081016040528093929190818152602001838380828437600081840152601f19601f820116905080830192505050505050509192919290803573ffffffffffffffffffffffffffffffffffffffff169060200190929190803560ff169060200190929190803590602001909291908035906020019092919050505061182e565b005b6107af6004803603602081101561078357600080fd5b81019080803573ffffffffffffffffffffffffffffffffffffffff169060200190929190505050611da4565b005b6060600060016000857dffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff19167dffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff191681526020019081526020016000206000847cffffffffffffffffffffffffffffffffffffffffffffffffffffffffff19167cffffffffffffffffffffffffffffffffffffffffffffffffffffffffff1916815260200190815260200160002060009054906101000a900473ffffffffffffffffffffffffffffffffffffffff169050600260008273ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1681526020019081526020016000206000018054600181600116156101000203166002900480601f0160208091040260200160405190810160405280929190818152602001828054600181600116156101000203166002900480156109545780601f1061092957610100808354040283529160200191610954565b820191906000526020600020905b81548152906001019060200180831161093757829003601f168201915b505050505091505092915050565b60008787878760405160200180857dffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff19167dffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff19168152600201847cffffffffffffffffffffffffffffffffffffffffffffffffffffffffff19167cffffffffffffffffffffffffffffffffffffffffffffffffffffffffff1916815260030183805190602001908083835b602083101515610a2e5780518252602082019150602081019050602083039250610a09565b6001836020036101000a0380198251168184511680821785525050505050509050018273ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff166c01000000000000000000000000028152601401945050505050604051602081830303815290604052805190602001209050600060016040805190810160405280601c81526020017f19457468657265756d205369676e6564204d6573736167653a0a333200000000815250836040516020018083805190602001908083835b602083101515610b225780518252602082019150602081019050602083039250610afd565b6001836020036101000a038019825116818451168082178552505050505050905001828152602001925050506040516020818303038152906040528051906020012086868660405160008152602001604052604051808581526020018460ff1660ff1681526020018381526020018281526020019450505050506020604051602081039080840390855afa158015610bbe573d6000803e3d6000fd5b5050506020604051035190508073ffffffffffffffffffffffffffffffffffffffff16600160008b7dffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff19167dffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff1916815260200190815260200160002060008a7cffffffffffffffffffffffffffffffffffffffffffffffffffffffffff19167cffffffffffffffffffffffffffffffffffffffffffffffffffffffffff1916815260200190815260200160002060009054906101000a900473ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff16141515610d16576040517f08c379a0000000000000000000000000000000000000000000000000000000008152600401808060200182810382526031815260200180611fab6031913960400191505060405180910390fd5b60408051908101604052808881526020018773ffffffffffffffffffffffffffffffffffffffff16815250600260008373ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1681526020019081526020016000206000820151816000019080519060200190610d9c929190611ebd565b5060208201518160010160006101000a81548173ffffffffffffffffffffffffffffffffffffffff021916908373ffffffffffffffffffffffffffffffffffffffff160217905550905050505050505050505050565b6000858560405160200180837dffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff19167dffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff19168152600201827cffffffffffffffffffffffffffffffffffffffffffffffffffffffffff19167cffffffffffffffffffffffffffffffffffffffffffffffffffffffffff1916815260030192505050604051602081830303815290604052805190602001209050600060016040805190810160405280601c81526020017f19457468657265756d205369676e6564204d6573736167653a0a333200000000815250836040516020018083805190602001908083835b602083101515610f1a5780518252602082019150602081019050602083039250610ef5565b6001836020036101000a038019825116818451168082178552505050505050905001828152602001925050506040516020818303038152906040528051906020012086868660405160008152602001604052604051808581526020018460ff1660ff1681526020018381526020018281526020019450505050506020604051602081039080840390855afa158015610fb6573d6000803e3d6000fd5b5050506020604051035190508073ffffffffffffffffffffffffffffffffffffffff1660016000897dffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff19167dffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff191681526020019081526020016000206000887cffffffffffffffffffffffffffffffffffffffffffffffffffffffffff19167cffffffffffffffffffffffffffffffffffffffffffffffffffffffffff1916815260200190815260200160002060009054906101000a900473ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1614151561110e576040517f08c379a0000000000000000000000000000000000000000000000000000000008152600401808060200182810382526033815260200180611fdc6033913960400191505060405180910390fd5b60016000887dffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff19167dffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff191681526020019081526020016000206000877cffffffffffffffffffffffffffffffffffffffffffffffffffffffffff19167cffffffffffffffffffffffffffffffffffffffffffffffffffffffffff1916815260200190815260200160002060006101000a81549073ffffffffffffffffffffffffffffffffffffffff0219169055600260008273ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff168152602001908152602001600020600080820160006112279190611f3d565b6001820160006101000a81549073ffffffffffffffffffffffffffffffffffffffff0219169055505050505050505050565b60008060016000857dffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff19167dffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff191681526020019081526020016000206000847cffffffffffffffffffffffffffffffffffffffffffffffffffffffffff19167cffffffffffffffffffffffffffffffffffffffffffffffffffffffffff1916815260200190815260200160002060009054906101000a900473ffffffffffffffffffffffffffffffffffffffff169050600260008273ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff16815260200190815260200160002060010160009054906101000a900473ffffffffffffffffffffffffffffffffffffffff1691505092915050565b6113996117d7565b15156113a457600080fd5b600060016000877dffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff19167dffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff191681526020019081526020016000206000867cffffffffffffffffffffffffffffffffffffffffffffffffffffffffff19167cffffffffffffffffffffffffffffffffffffffffffffffffffffffffff1916815260200190815260200160002060009054906101000a900473ffffffffffffffffffffffffffffffffffffffff1690508360016000887dffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff19167dffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff191681526020019081526020016000206000877cffffffffffffffffffffffffffffffffffffffffffffffffffffffffff19167cffffffffffffffffffffffffffffffffffffffffffffffffffffffffff1916815260200190815260200160002060006101000a81548173ffffffffffffffffffffffffffffffffffffffff021916908373ffffffffffffffffffffffffffffffffffffffff16021790555060408051908101604052808481526020018373ffffffffffffffffffffffffffffffffffffffff16815250600260008673ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff16815260200190815260200160002060008201518160000190805190602001906115da929190611ebd565b5060208201518160010160006101000a81548173ffffffffffffffffffffffffffffffffffffffff021916908373ffffffffffffffffffffffffffffffffffffffff1602179055509050508073ffffffffffffffffffffffffffffffffffffffff168473ffffffffffffffffffffffffffffffffffffffff161415156116d457600260008273ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff168152602001908152602001600020600080820160006116aa9190611f3d565b6001820160006101000a81549073ffffffffffffffffffffffffffffffffffffffff021916905550505b505050505050565b6116e46117d7565b15156116ef57600080fd5b600073ffffffffffffffffffffffffffffffffffffffff166000809054906101000a900473ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff167f8be0079c531659141344cd1fd0a4f28419497f9722a3daafe3b4186f6b6457e060405160405180910390a360008060006101000a81548173ffffffffffffffffffffffffffffffffffffffff021916908373ffffffffffffffffffffffffffffffffffffffff160217905550565b60008060009054906101000a900473ffffffffffffffffffffffffffffffffffffffff16905090565b60008060009054906101000a900473ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff163373ffffffffffffffffffffffffffffffffffffffff1614905090565b600073ffffffffffffffffffffffffffffffffffffffff1660016000897dffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff19167dffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff191681526020019081526020016000206000887cffffffffffffffffffffffffffffffffffffffffffffffffffffffffff19167cffffffffffffffffffffffffffffffffffffffffffffffffffffffffff1916815260200190815260200160002060009054906101000a900473ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1614151561197b576040517f08c379a000000000000000000000000000000000000000000000000000000000815260040180806020018281038252602381526020018061200f6023913960400191505060405180910390fd5b60008787878760405160200180857dffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff19167dffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff19168152600201847cffffffffffffffffffffffffffffffffffffffffffffffffffffffffff19167cffffffffffffffffffffffffffffffffffffffffffffffffffffffffff1916815260030183805190602001908083835b602083101515611a475780518252602082019150602081019050602083039250611a22565b6001836020036101000a0380198251168184511680821785525050505050509050018273ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff166c01000000000000000000000000028152601401945050505050604051602081830303815290604052805190602001209050600060016040805190810160405280601c81526020017f19457468657265756d205369676e6564204d6573736167653a0a333200000000815250836040516020018083805190602001908083835b602083101515611b3b5780518252602082019150602081019050602083039250611b16565b6001836020036101000a038019825116818451168082178552505050505050905001828152602001925050506040516020818303038152906040528051906020012086868660405160008152602001604052604051808581526020018460ff1660ff1681526020018381526020018281526020019450505050506020604051602081039080840390855afa158015611bd7573d6000803e3d6000fd5b50505060206040510351905080600160008b7dffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff19167dffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff1916815260200190815260200160002060008a7cffffffffffffffffffffffffffffffffffffffffffffffffffffffffff19167cffffffffffffffffffffffffffffffffffffffffffffffffffffffffff1916815260200190815260200160002060006101000a81548173ffffffffffffffffffffffffffffffffffffffff021916908373ffffffffffffffffffffffffffffffffffffffff16021790555060408051908101604052808881526020018773ffffffffffffffffffffffffffffffffffffffff16815250600260008373ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1681526020019081526020016000206000820151816000019080519060200190611d4e929190611ebd565b5060208201518160010160006101000a81548173ffffffffffffffffffffffffffffffffffffffff021916908373ffffffffffffffffffffffffffffffffffffffff160217905550905050505050505050505050565b611dac6117d7565b1515611db757600080fd5b611dc081611dc3565b50565b600073ffffffffffffffffffffffffffffffffffffffff168173ffffffffffffffffffffffffffffffffffffffff1614151515611dff57600080fd5b8073ffffffffffffffffffffffffffffffffffffffff166000809054906101000a900473ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff167f8be0079c531659141344cd1fd0a4f28419497f9722a3daafe3b4186f6b6457e060405160405180910390a3806000806101000a81548173ffffffffffffffffffffffffffffffffffffffff021916908373ffffffffffffffffffffffffffffffffffffffff16021790555050565b828054600181600116156101000203166002900490600052602060002090601f016020900481019282601f10611efe57805160ff1916838001178555611f2c565b82800160010185558215611f2c579182015b82811115611f2b578251825591602001919060010190611f10565b5b509050611f399190611f85565b5090565b50805460018160011615610100020316600290046000825580601f10611f635750611f82565b601f016020900490600052602060002090810190611f819190611f85565b5b50565b611fa791905b80821115611fa3576000816000905550600101611f8b565b5090565b9056fe556e617574686f72697a656420746f20757064617465207468697320656e74727920696e20746865207265676973747279556e617574686f72697a656420746f2072656d6f7665207468697320656e7472792066726f6d20746865207265676973747279506172747920494420616c72656164792065786973747320696e207265676973747279a165627a7a72305820d1203db73e7617a9469e47e6cf40f7898cb13b92918b7a6108b3a8576ffad3ae0029";

    public static final String FUNC_RENOUNCEOWNERSHIP = "renounceOwnership";

    public static final String FUNC_OWNER = "owner";

    public static final String FUNC_ISOWNER = "isOwner";

    public static final String FUNC_TRANSFEROWNERSHIP = "transferOwnership";

    public static final String FUNC_ADMINOVERWRITE = "adminOverwrite";

    public static final String FUNC_REGISTER = "register";

    public static final String FUNC_DEREGISTER = "deregister";

    public static final String FUNC_UPDATECLIENTINFO = "updateClientInfo";

    public static final String FUNC_CLIENTURLOF = "clientURLOf";

    public static final String FUNC_CLIENTADDRESSOF = "clientAddressOf";

    public static final Event OWNERSHIPTRANSFERRED_EVENT = new Event("OwnershipTransferred",
            Arrays.<TypeReference<?>>asList(new TypeReference<Address>(true) {}, new TypeReference<Address>(true) {}));
    ;

    protected static final HashMap<String, String> _addresses;

    static {
        _addresses = new HashMap<String, String>();
        _addresses.put("9", "0xE843CDE33060bf9CB11723934EAD6a3DE410DdEE");
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

    public RemoteCall<TransactionReceipt> adminOverwrite(byte[] countryCode, byte[] partyID, String newRoleAddress, String newClientURL, String newClientAddress) {
        final Function function = new Function(
                FUNC_ADMINOVERWRITE,
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Bytes2(countryCode),
                        new org.web3j.abi.datatypes.generated.Bytes3(partyID),
                        new org.web3j.abi.datatypes.Address(newRoleAddress),
                        new org.web3j.abi.datatypes.Utf8String(newClientURL),
                        new org.web3j.abi.datatypes.Address(newClientAddress)),
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteCall<TransactionReceipt> register(byte[] countryCode, byte[] partyID, String clientURL, String clientAddress, BigInteger v, byte[] r, byte[] s) {
        final Function function = new Function(
                FUNC_REGISTER,
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Bytes2(countryCode),
                        new org.web3j.abi.datatypes.generated.Bytes3(partyID),
                        new org.web3j.abi.datatypes.Utf8String(clientURL),
                        new org.web3j.abi.datatypes.Address(clientAddress),
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

    public RemoteCall<TransactionReceipt> updateClientInfo(byte[] countryCode, byte[] partyID, String newClientURL, String newClientAddress, BigInteger v, byte[] r, byte[] s) {
        final Function function = new Function(
                FUNC_UPDATECLIENTINFO,
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Bytes2(countryCode),
                        new org.web3j.abi.datatypes.generated.Bytes3(partyID),
                        new org.web3j.abi.datatypes.Utf8String(newClientURL),
                        new org.web3j.abi.datatypes.Address(newClientAddress),
                        new org.web3j.abi.datatypes.generated.Uint8(v),
                        new org.web3j.abi.datatypes.generated.Bytes32(r),
                        new org.web3j.abi.datatypes.generated.Bytes32(s)),
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteCall<String> clientURLOf(byte[] countryCode, byte[] partyID) {
        final Function function = new Function(FUNC_CLIENTURLOF,
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Bytes2(countryCode),
                        new org.web3j.abi.datatypes.generated.Bytes3(partyID)),
                Arrays.<TypeReference<?>>asList(new TypeReference<Utf8String>() {}));
        return executeRemoteCallSingleValueReturn(function, String.class);
    }

    public RemoteCall<String> clientAddressOf(byte[] countryCode, byte[] partyID) {
        final Function function = new Function(FUNC_CLIENTADDRESSOF,
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
