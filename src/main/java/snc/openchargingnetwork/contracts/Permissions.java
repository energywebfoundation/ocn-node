package snc.openchargingnetwork.contracts;

import io.reactivex.Flowable;
import io.reactivex.functions.Function;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Callable;
import org.web3j.abi.EventEncoder;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.DynamicArray;
import org.web3j.abi.datatypes.Event;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.Utf8String;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.RemoteCall;
import org.web3j.protocol.core.RemoteFunctionCall;
import org.web3j.protocol.core.methods.request.EthFilter;
import org.web3j.protocol.core.methods.response.BaseEventResponse;
import org.web3j.protocol.core.methods.response.Log;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.tuples.generated.Tuple3;
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
 * <p>Generated with web3j version 4.5.16.
 */
@SuppressWarnings("rawtypes")
public class Permissions extends Contract {
    public static final String BINARY = "0x608060405234801561001057600080fd5b50604051611db4380380611db48339818101604052602081101561003357600080fd5b8101908080519060200190929190505050806000806101000a81548173ffffffffffffffffffffffffffffffffffffffff021916908373ffffffffffffffffffffffffffffffffffffffff16021790555050611d20806100946000396000f3fe608060405234801561001057600080fd5b50600436106100885760003560e01c8063adb9cae61161005b578063adb9cae6146103ab578063c13489ca14610444578063dc3acebe1461062a578063edc922a91461079b57610088565b8063099e97621461008d578063340f1e7c1461029457806350f3fc81146102f9578063884eb94914610367575b600080fd5b610292600480360360c08110156100a357600080fd5b81019080803590602001906401000000008111156100c057600080fd5b8201836020820111156100d257600080fd5b803590602001918460018302840111640100000000831117156100f457600080fd5b91908080601f016020809104026020016040519081016040528093929190818152602001838380828437600081840152601f19601f8201169050808301925050505050505091929192908035906020019064010000000081111561015757600080fd5b82018360208201111561016957600080fd5b8035906020019184600183028401116401000000008311171561018b57600080fd5b91908080601f016020809104026020016040519081016040528093929190818152602001838380828437600081840152601f19601f820116905080830192505050505050509192919290803590602001906401000000008111156101ee57600080fd5b82018360208201111561020057600080fd5b8035906020019184602083028401116401000000008311171561022257600080fd5b919080806020026020016040519081016040528093929190818152602001838360200280828437600081840152601f19601f820116905080830192505050505050509192919290803560ff16906020019092919080359060200190929190803590602001909291905050506107fa565b005b6102f7600480360360808110156102aa57600080fd5b81019080803573ffffffffffffffffffffffffffffffffffffffff169060200190929190803560ff1690602001909291908035906020019092919080359060200190929190505050610a27565b005b6103256004803603602081101561030f57600080fd5b8101908080359060200190929190505050610bac565b604051808273ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff16815260200191505060405180910390f35b6103a96004803603602081101561037d57600080fd5b81019080803573ffffffffffffffffffffffffffffffffffffffff169060200190929190505050610be8565b005b6103ed600480360360208110156103c157600080fd5b81019080803573ffffffffffffffffffffffffffffffffffffffff169060200190929190505050610bf5565b6040518080602001828103825283818151815260200191508051906020019060200280838360005b83811015610430578082015181840152602081019050610415565b505050509050019250505060405180910390f35b6106286004803603606081101561045a57600080fd5b810190808035906020019064010000000081111561047757600080fd5b82018360208201111561048957600080fd5b803590602001918460018302840111640100000000831117156104ab57600080fd5b91908080601f016020809104026020016040519081016040528093929190818152602001838380828437600081840152601f19601f8201169050808301925050505050505091929192908035906020019064010000000081111561050e57600080fd5b82018360208201111561052057600080fd5b8035906020019184600183028401116401000000008311171561054257600080fd5b91908080601f016020809104026020016040519081016040528093929190818152602001838380828437600081840152601f19601f820116905080830192505050505050509192919290803590602001906401000000008111156105a557600080fd5b8201836020820111156105b757600080fd5b803590602001918460208302840111640100000000831117156105d957600080fd5b919080806020026020016040519081016040528093929190818152602001838360200280828437600081840152601f19601f820116905080830192505050505050509192919290505050610cc2565b005b61066c6004803603602081101561064057600080fd5b81019080803573ffffffffffffffffffffffffffffffffffffffff169060200190929190505050610cd3565b60405180806020018060200180602001848103845287818151815260200191508051906020019080838360005b838110156106b4578082015181840152602081019050610699565b50505050905090810190601f1680156106e15780820380516001836020036101000a031916815260200191505b50848103835286818151815260200191508051906020019080838360005b8381101561071a5780820151818401526020810190506106ff565b50505050905090810190601f1680156107475780820380516001836020036101000a031916815260200191505b50848103825285818151815260200191508051906020019060200280838360005b83811015610783578082015181840152602081019050610768565b50505050905001965050505050505060405180910390f35b6107a3610f2c565b6040518080602001828103825283818151815260200191508051906020019060200280838360005b838110156107e65780820151818401526020810190506107cb565b505050509050019250505060405180910390f35b60008686866040516020018084805190602001908083835b602083106108355780518252602082019150602081019050602083039250610812565b6001836020036101000a03801982511681845116808217855250505050505090500183805190602001908083835b602083106108865780518252602082019150602081019050602083039250610863565b6001836020036101000a038019825116818451168082178552505050505050905001828051906020019060200280838360005b838110156108d45780820151818401526020810190506108b9565b505050509050019350505050604051602081830303815290604052805190602001209050600060016040518060400160405280601c81526020017f19457468657265756d205369676e6564204d6573736167653a0a333200000000815250836040516020018083805190602001908083835b602083106109695780518252602082019150602081019050602083039250610946565b6001836020036101000a038019825116818451168082178552505050505050905001828152602001925050506040516020818303038152906040528051906020012086868660405160008152602001604052604051808581526020018460ff1660ff1681526020018381526020018281526020019450505050506020604051602081039080840390855afa158015610a05573d6000803e3d6000fd5b505050602060405103519050610a1d81898989610fba565b5050505050505050565b600084604051602001808273ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1660601b8152601401915050604051602081830303815290604052805190602001209050600060016040518060400160405280601c81526020017f19457468657265756d205369676e6564204d6573736167653a0a333200000000815250836040516020018083805190602001908083835b60208310610af25780518252602082019150602081019050602083039250610acf565b6001836020036101000a038019825116818451168082178552505050505050905001828152602001925050506040516020818303038152906040528051906020012086868660405160008152602001604052604051808581526020018460ff1660ff1681526020018381526020018281526020019450505050506020604051602081039080840390855afa158015610b8e573d6000803e3d6000fd5b505050602060405103519050610ba481876115b6565b505050505050565b60018181548110610bb957fe5b906000526020600020016000915054906101000a900473ffffffffffffffffffffffffffffffffffffffff1681565b610bf233826115b6565b50565b6060600560008373ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff168152602001908152602001600020805480602002602001604051908101604052809291908181526020018280548015610cb657602002820191906000526020600020905b8160009054906101000a900473ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1681526020019060010190808311610c6c575b50505050509050919050565b610cce33848484610fba565b505050565b6060806060600360008573ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1681526020019081526020016000206000018054600181600116156101000203166002900480601f016020809104026020016040519081016040528092919081815260200182805460018160011615610100020316600290048015610dae5780601f10610d8357610100808354040283529160200191610dae565b820191906000526020600020905b815481529060010190602001808311610d9157829003601f168201915b50505050509250600360008573ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1681526020019081526020016000206001018054600181600116156101000203166002900480601f016020809104026020016040519081016040528092919081815260200182805460018160011615610100020316600290048015610e8b5780601f10610e6057610100808354040283529160200191610e8b565b820191906000526020600020905b815481529060010190602001808311610e6e57829003601f168201915b50505050509150600360008573ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff168152602001908152602001600020600201805480602002602001604051908101604052809291908181526020018280548015610f1e57602002820191906000526020600020905b815481526020019060010190808311610f0a575b505050505090509193909250565b60606001805480602002602001604051908101604052809291908181526020018280548015610fb057602002820191906000526020600020905b8160009054906101000a900473ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1681526020019060010190808311610f66575b5050505050905090565b60008060009054906101000a900473ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff166388cf72a0866040518263ffffffff1660e01b8152600401808273ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff16815260200191505060006040518083038186803b15801561105a57600080fd5b505afa15801561106e573d6000803e3d6000fd5b505050506040513d6000823e3d601f19601f82011682018060405250604081101561109857600080fd5b8101908080519060200190929190805160405193929190846401000000008211156110c257600080fd5b838201915060208201858111156110d857600080fd5b82518660018202830111640100000000821117156110f557600080fd5b8083526020830192505050908051906020019080838360005b8381101561112957808201518184015260208101905061110e565b50505050905090810190601f1680156111565780820380516001836020036101000a031916815260200191505b50604052505050509050600073ffffffffffffffffffffffffffffffffffffffff168173ffffffffffffffffffffffffffffffffffffffff1614156111e6576040517f08c379a000000000000000000000000000000000000000000000000000000000815260040180806020018281038252603c815260200180611cb0603c913960400191505060405180910390fd5b600082511161125d576040517f08c379a00000000000000000000000000000000000000000000000000000000081526004018080602001828103825260158152602001807f4e6f207065726d697373696f6e7320676976656e2e000000000000000000000081525060200191505060405180910390fd5b604051806060016040528085815260200184815260200183815250600360008773ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff16815260200190815260200160002060008201518160000190805190602001906112d3929190611b62565b5060208201518160010190805190602001906112f0929190611b62565b50604082015181600201908051906020019061130d929190611be2565b5090505060001515600260008773ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff16815260200190815260200160002060009054906101000a900460ff16151514156114295760018590806001815401808255809150509060018203906000526020600020016000909192909190916101000a81548173ffffffffffffffffffffffffffffffffffffffff021916908373ffffffffffffffffffffffffffffffffffffffff160217905550506001600260008773ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff16815260200190815260200160002060006101000a81548160ff0219169083151502179055505b7f5217645fd1a5d80a7a561ee8df6541e94bff9b1151d8c1281e17c1a304d9d6fa84848488604051808060200180602001806020018573ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff168152602001848103845288818151815260200191508051906020019080838360005b838110156114c85780820151818401526020810190506114ad565b50505050905090810190601f1680156114f55780820380516001836020036101000a031916815260200191505b50848103835287818151815260200191508051906020019080838360005b8381101561152e578082015181840152602081019050611513565b50505050905090810190601f16801561155b5780820380516001836020036101000a031916815260200191505b50848103825286818151815260200191508051906020019060200280838360005b8381101561159757808201518184015260208101905061157c565b5050505090500197505050505050505060405180910390a15050505050565b60008060009054906101000a900473ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff166388cf72a0846040518263ffffffff1660e01b8152600401808273ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff16815260200191505060006040518083038186803b15801561165657600080fd5b505afa15801561166a573d6000803e3d6000fd5b505050506040513d6000823e3d601f19601f82011682018060405250604081101561169457600080fd5b8101908080519060200190929190805160405193929190846401000000008211156116be57600080fd5b838201915060208201858111156116d457600080fd5b82518660018202830111640100000000821117156116f157600080fd5b8083526020830192505050908051906020019080838360005b8381101561172557808201518184015260208101905061170a565b50505050905090810190601f1680156117525780820380516001836020036101000a031916815260200191505b50604052505050509050600073ffffffffffffffffffffffffffffffffffffffff168173ffffffffffffffffffffffffffffffffffffffff1614156117e2576040517f08c379a000000000000000000000000000000000000000000000000000000000815260040180806020018281038252602a815260200180611c55602a913960400191505060405180910390fd5b60011515600260008473ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff16815260200190815260200160002060009054906101000a900460ff161515146118a8576040517f08c379a000000000000000000000000000000000000000000000000000000000815260040180806020018281038252601f8152602001807f50726f766964657220686173206e6f2072656769737465726564204170702e0081525060200191505060405180910390fd5b60001515600460008573ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff16815260200190815260200160002060008473ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff16815260200190815260200160002060009054906101000a900460ff1615151461198e576040517f08c379a0000000000000000000000000000000000000000000000000000000008152600401808060200182810382526031815260200180611c7f6031913960400191505060405180910390fd5b6001600460008573ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff16815260200190815260200160002060008473ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff16815260200190815260200160002060006101000a81548160ff021916908315150217905550600560008473ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1681526020019081526020016000208290806001815401808255809150509060018203906000526020600020016000909192909190916101000a81548173ffffffffffffffffffffffffffffffffffffffff021916908373ffffffffffffffffffffffffffffffffffffffff160217905550507f59c05fe4657968f976c095206514e77e8ed37c2fe56e7614cd8f245dfbb008298383604051808373ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1681526020018273ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1681526020019250505060405180910390a1505050565b828054600181600116156101000203166002900490600052602060002090601f016020900481019282601f10611ba357805160ff1916838001178555611bd1565b82800160010185558215611bd1579182015b82811115611bd0578251825591602001919060010190611bb5565b5b509050611bde9190611c2f565b5090565b828054828255906000526020600020908101928215611c1e579160200282015b82811115611c1d578251825591602001919060010190611c02565b5b509050611c2b9190611c2f565b5090565b611c5191905b80821115611c4d576000816000905550600101611c35565b5090565b9056fe417070207573657220686173206e6f207061727479206c697374696e6720696e2052656769737472792e41677265656d656e7420616c7265616479206d616465206265747765656e207573657220616e642070726f76696465722e547279696e6720746f20726567697374657220616e2061707020776974686f7574207061727479206c697374696e6720696e2052656769737472792ea265627a7a72315820d4d1ad154860d44803cd80584f7935eb25c503b78eaaa5930742156da612405d64736f6c634300050f0032";

    public static final String FUNC_PROVIDERS = "providers";

    public static final String FUNC_SETAPP = "setApp";

    public static final String FUNC_SETAPPRAW = "setAppRaw";

    public static final String FUNC_GETAPP = "getApp";

    public static final String FUNC_GETPROVIDERS = "getProviders";

    public static final String FUNC_CREATEAGREEMENT = "createAgreement";

    public static final String FUNC_CREATEAGREEMENTRAW = "createAgreementRaw";

    public static final String FUNC_GETUSERAGREEMENTS = "getUserAgreements";

    public static final Event APPAGREEMENT_EVENT = new Event("AppAgreement", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}, new TypeReference<Address>() {}));
    ;

    public static final Event APPUPDATE_EVENT = new Event("AppUpdate", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Utf8String>() {}, new TypeReference<Utf8String>() {}, new TypeReference<DynamicArray<Uint256>>() {}, new TypeReference<Address>() {}));
    ;

    protected static final HashMap<String, String> _addresses;

    static {
        _addresses = new HashMap<String, String>();
        _addresses.put("73799", "0x520896B666fCcDb6458D4eC5C1FdD0D6d9EB97A3");
        _addresses.put("9", "0xf25186B5081Ff5cE73482AD761DB0eB0d25abfBF");
    }

    @Deprecated
    protected Permissions(String contractAddress, Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    protected Permissions(String contractAddress, Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider) {
        super(BINARY, contractAddress, web3j, credentials, contractGasProvider);
    }

    @Deprecated
    protected Permissions(String contractAddress, Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    protected Permissions(String contractAddress, Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        super(BINARY, contractAddress, web3j, transactionManager, contractGasProvider);
    }

    public List<AppAgreementEventResponse> getAppAgreementEvents(TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = extractEventParametersWithLog(APPAGREEMENT_EVENT, transactionReceipt);
        ArrayList<AppAgreementEventResponse> responses = new ArrayList<AppAgreementEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            AppAgreementEventResponse typedResponse = new AppAgreementEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.user = (String) eventValues.getNonIndexedValues().get(0).getValue();
            typedResponse.provider = (String) eventValues.getNonIndexedValues().get(1).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public Flowable<AppAgreementEventResponse> appAgreementEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(new Function<Log, AppAgreementEventResponse>() {
            @Override
            public AppAgreementEventResponse apply(Log log) {
                Contract.EventValuesWithLog eventValues = extractEventParametersWithLog(APPAGREEMENT_EVENT, log);
                AppAgreementEventResponse typedResponse = new AppAgreementEventResponse();
                typedResponse.log = log;
                typedResponse.user = (String) eventValues.getNonIndexedValues().get(0).getValue();
                typedResponse.provider = (String) eventValues.getNonIndexedValues().get(1).getValue();
                return typedResponse;
            }
        });
    }

    public Flowable<AppAgreementEventResponse> appAgreementEventFlowable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(APPAGREEMENT_EVENT));
        return appAgreementEventFlowable(filter);
    }

    public List<AppUpdateEventResponse> getAppUpdateEvents(TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = extractEventParametersWithLog(APPUPDATE_EVENT, transactionReceipt);
        ArrayList<AppUpdateEventResponse> responses = new ArrayList<AppUpdateEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            AppUpdateEventResponse typedResponse = new AppUpdateEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.name = (String) eventValues.getNonIndexedValues().get(0).getValue();
            typedResponse.url = (String) eventValues.getNonIndexedValues().get(1).getValue();
            typedResponse.permissions = (List<BigInteger>) eventValues.getNonIndexedValues().get(2).getValue();
            typedResponse.provider = (String) eventValues.getNonIndexedValues().get(3).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public Flowable<AppUpdateEventResponse> appUpdateEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(new Function<Log, AppUpdateEventResponse>() {
            @Override
            public AppUpdateEventResponse apply(Log log) {
                Contract.EventValuesWithLog eventValues = extractEventParametersWithLog(APPUPDATE_EVENT, log);
                AppUpdateEventResponse typedResponse = new AppUpdateEventResponse();
                typedResponse.log = log;
                typedResponse.name = (String) eventValues.getNonIndexedValues().get(0).getValue();
                typedResponse.url = (String) eventValues.getNonIndexedValues().get(1).getValue();
                typedResponse.permissions = (List<BigInteger>) eventValues.getNonIndexedValues().get(2).getValue();
                typedResponse.provider = (String) eventValues.getNonIndexedValues().get(3).getValue();
                return typedResponse;
            }
        });
    }

    public Flowable<AppUpdateEventResponse> appUpdateEventFlowable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(APPUPDATE_EVENT));
        return appUpdateEventFlowable(filter);
    }

    public RemoteFunctionCall<String> providers(BigInteger param0) {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(FUNC_PROVIDERS, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(param0)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}));
        return executeRemoteCallSingleValueReturn(function, String.class);
    }

    public RemoteFunctionCall<TransactionReceipt> setApp(String name, String url, List<BigInteger> permissions) {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(
                FUNC_SETAPP, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Utf8String(name), 
                new org.web3j.abi.datatypes.Utf8String(url), 
                new org.web3j.abi.datatypes.DynamicArray<org.web3j.abi.datatypes.generated.Uint256>(
                        org.web3j.abi.datatypes.generated.Uint256.class,
                        org.web3j.abi.Utils.typeMap(permissions, org.web3j.abi.datatypes.generated.Uint256.class))), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> setAppRaw(String name, String url, List<BigInteger> permissions, BigInteger v, byte[] r, byte[] s) {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(
                FUNC_SETAPPRAW, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Utf8String(name), 
                new org.web3j.abi.datatypes.Utf8String(url), 
                new org.web3j.abi.datatypes.DynamicArray<org.web3j.abi.datatypes.generated.Uint256>(
                        org.web3j.abi.datatypes.generated.Uint256.class,
                        org.web3j.abi.Utils.typeMap(permissions, org.web3j.abi.datatypes.generated.Uint256.class)), 
                new org.web3j.abi.datatypes.generated.Uint8(v), 
                new org.web3j.abi.datatypes.generated.Bytes32(r), 
                new org.web3j.abi.datatypes.generated.Bytes32(s)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<Tuple3<String, String, List<BigInteger>>> getApp(String provider) {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(FUNC_GETAPP, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(provider)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Utf8String>() {}, new TypeReference<Utf8String>() {}, new TypeReference<DynamicArray<Uint256>>() {}));
        return new RemoteFunctionCall<Tuple3<String, String, List<BigInteger>>>(function,
                new Callable<Tuple3<String, String, List<BigInteger>>>() {
                    @Override
                    public Tuple3<String, String, List<BigInteger>> call() throws Exception {
                        List<Type> results = executeCallMultipleValueReturn(function);
                        return new Tuple3<String, String, List<BigInteger>>(
                                (String) results.get(0).getValue(), 
                                (String) results.get(1).getValue(), 
                                convertToNative((List<Uint256>) results.get(2).getValue()));
                    }
                });
    }

    public RemoteFunctionCall<List> getProviders() {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(FUNC_GETPROVIDERS, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<DynamicArray<Address>>() {}));
        return new RemoteFunctionCall<List>(function,
                new Callable<List>() {
                    @Override
                    @SuppressWarnings("unchecked")
                    public List call() throws Exception {
                        List<Type> result = (List<Type>) executeCallSingleValueReturn(function, List.class);
                        return convertToNative(result);
                    }
                });
    }

    public RemoteFunctionCall<TransactionReceipt> createAgreement(String provider) {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(
                FUNC_CREATEAGREEMENT, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(provider)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> createAgreementRaw(String provider, BigInteger v, byte[] r, byte[] s) {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(
                FUNC_CREATEAGREEMENTRAW, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(provider), 
                new org.web3j.abi.datatypes.generated.Uint8(v), 
                new org.web3j.abi.datatypes.generated.Bytes32(r), 
                new org.web3j.abi.datatypes.generated.Bytes32(s)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<List> getUserAgreements(String user) {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(FUNC_GETUSERAGREEMENTS, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(user)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<DynamicArray<Address>>() {}));
        return new RemoteFunctionCall<List>(function,
                new Callable<List>() {
                    @Override
                    @SuppressWarnings("unchecked")
                    public List call() throws Exception {
                        List<Type> result = (List<Type>) executeCallSingleValueReturn(function, List.class);
                        return convertToNative(result);
                    }
                });
    }

    @Deprecated
    public static Permissions load(String contractAddress, Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        return new Permissions(contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    @Deprecated
    public static Permissions load(String contractAddress, Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        return new Permissions(contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    public static Permissions load(String contractAddress, Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider) {
        return new Permissions(contractAddress, web3j, credentials, contractGasProvider);
    }

    public static Permissions load(String contractAddress, Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        return new Permissions(contractAddress, web3j, transactionManager, contractGasProvider);
    }

    public static RemoteCall<Permissions> deploy(Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider, String registryAddress) {
        String encodedConstructor = FunctionEncoder.encodeConstructor(Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(registryAddress)));
        return deployRemoteCall(Permissions.class, web3j, credentials, contractGasProvider, BINARY, encodedConstructor);
    }

    public static RemoteCall<Permissions> deploy(Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider, String registryAddress) {
        String encodedConstructor = FunctionEncoder.encodeConstructor(Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(registryAddress)));
        return deployRemoteCall(Permissions.class, web3j, transactionManager, contractGasProvider, BINARY, encodedConstructor);
    }

    @Deprecated
    public static RemoteCall<Permissions> deploy(Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit, String registryAddress) {
        String encodedConstructor = FunctionEncoder.encodeConstructor(Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(registryAddress)));
        return deployRemoteCall(Permissions.class, web3j, credentials, gasPrice, gasLimit, BINARY, encodedConstructor);
    }

    @Deprecated
    public static RemoteCall<Permissions> deploy(Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit, String registryAddress) {
        String encodedConstructor = FunctionEncoder.encodeConstructor(Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(registryAddress)));
        return deployRemoteCall(Permissions.class, web3j, transactionManager, gasPrice, gasLimit, BINARY, encodedConstructor);
    }

    protected String getStaticDeployedAddress(String networkId) {
        return _addresses.get(networkId);
    }

    public static String getPreviouslyDeployedAddress(String networkId) {
        return _addresses.get(networkId);
    }

    public static class AppAgreementEventResponse extends BaseEventResponse {
        public String user;

        public String provider;
    }

    public static class AppUpdateEventResponse extends BaseEventResponse {
        public String name;

        public String url;

        public List<BigInteger> permissions;

        public String provider;
    }
}
