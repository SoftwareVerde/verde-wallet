//
//  Use this file to import your target's public headers that you would like to expose to Swift.
//

#ifndef BRIDGING_HEADER_H
#define BRIDGING_HEADER_H

#include "J2ObjC_header.h"

#include "JavaObject.h"
#include "java/lang/Boolean.h"
#include "java/lang/Integer.h"
#include "java/lang/Runnable.h"

#include "IOSClass.h"
#include "IOSObjectArray.h"
#include "J2ObjC_source.h"

#include "java/io/ByteArrayInputStream.h"
#include "java/io/PrintStream.h"
#include "java/io/File.h"
#include "java/lang/Boolean.h"
#include "java/lang/Double.h"
#include "java/lang/Exception.h"
#include "java/lang/Float.h"
#include "java/lang/Integer.h"
#include "java/lang/Long.h"
#include "java/lang/Math.h"
#include "java/lang/Runnable.h"
#include "java/lang/RuntimeException.h"
#include "java/lang/StringBuilder.h"
#include "java/lang/System.h"
#include "java/lang/Thread.h"
#include "java/lang/AutoCloseable.h"
#include "java/org/bouncycastle/openpgp/PGPPublicKeyRing.h"
#include "java/org/bouncycastle/openpgp/PGPSecretKeyRing.h"
#include "java/org/bitcoin/Secp256k1Context.h"
#include "java/util/ArrayList.h"
#include "java/util/Collection.h"
#include "java/util/HashMap.h"
#include "java/util/LinkedList.h"
#include "java/util/List.h"
#include "java/util/Map.h"
#include "java/util/Set.h"

#include "IOSPrimitiveArray.h"
#include "JavaObject.h"
#include "NSObject+JavaObject.h"
#include "java/io/InputStream.h"
#include "java/io/OutputStream.h"
#include "java/io/Reader.h"
#include "java/io/Writer.h"
#include "java/sql/Array.h"
#include "java/sql/Blob.h"
#include "java/sql/CallableStatement.h"
#include "java/sql/Clob.h"
#include "java/sql/Connection.h"
#include "java/sql/DatabaseMetaData.h"
#include "java/sql/NClob.h"
#include "java/sql/PreparedStatement.h"
#include "java/sql/ResultSet.h"
#include "java/sql/RowIdLifetime.h"
#include "java/sql/Savepoint.h"
#include "java/sql/Statement.h"
#include "java/sql/Struct.h"
#include "java/sql/SQLXML.h"
#include "java/sql/SQLException.h"
#include "java/sql/SQLWarning.h"
#include "java/sql/Wrapper.h"
#include "java/util/Properties.h"
#include "java/util/Collections.h"
#include "javax/xml/transform/Result.h"
#include "javax/xml/transform/Source.h"
#include "com/softwareverde/bitcoin/address/Address.h"
#include "com/softwareverde/bitcoin/address/CompressedAddress.h"
#include "com/softwareverde/bitcoin/address/AddressInflater.h"
#include "com/softwareverde/bitcoin/app/lib/BitcoinVerde.h"
#include "com/softwareverde/bitcoin/app/lib/KeyStore.h"
#include "com/softwareverde/bitcoin/app/lib/MerkleBlockSyncUpdateCallback.h"
#include "com/softwareverde/bitcoin/jni/NativeUnspentTransactionOutputCache.h"
#include "com/softwareverde/bitcoin/jni/NativeSecp256k1.h"
#include "com/softwareverde/network/p2p/node/address/NodeIpAddress.h"
#include "com/softwareverde/bitcoin/slp/SlpTokenId.h"
#include "com/softwareverde/bitcoin/server/configuration/SeedNodeProperties.h"
#include "com/softwareverde/bitcoin/server/database/Database.h"
#include "com/softwareverde/bitcoin/server/database/DatabaseConnectionFactory.h"
#include "com/softwareverde/bitcoin/server/database/DatabaseConnectionCore.h"
#include "com/softwareverde/bitcoin/server/database/DatabaseConnection.h"
#include "com/softwareverde/bitcoin/transaction/Transaction.h"
#include "com/softwareverde/bitcoin/transaction/TransactionDeflater.h"
#include "com/softwareverde/bitcoin/transaction/MutableTransaction.h"
#include "com/softwareverde/bitcoin/transaction/input/MutableTransactionInput.h"
#include "com/softwareverde/bitcoin/transaction/output/identifier/TransactionOutputIdentifier.h"
#include "com/softwareverde/bitcoin/transaction/script/locking/LockingScript.h"
#include "com/softwareverde/bitcoin/transaction/script/ScriptPatternMatcher.h"
#include "com/softwareverde/bitcoin/wallet/Wallet.h"
#include "com/softwareverde/bitcoin/wallet/PaymentAmount.h"
#include "com/softwareverde/bitcoin/wallet/slp/SlpPaymentAmount.h"
#include "com/softwareverde/bitcoin/wallet/utxo/SpendableTransactionOutput.h"
#include "com/softwareverde/constable/list/mutable/MutableList.h"
#include "com/softwareverde/constable/list/immutable/ImmutableList.h"
#include "com/softwareverde/database/DatabaseConnectionFactory.h"
#include "com/softwareverde/database/DatabaseConnection.h"
#include "com/softwareverde/database/DatabaseException.h"
#include "com/softwareverde/database/query/Query.h"
#include "com/softwareverde/database/row/Row.h"
#include "com/softwareverde/database/query/parameter/ParameterType.h"
#include "com/softwareverde/database/query/parameter/TypedParameter.h"

#include "com/softwareverde/json/Json.h"
#include "com/softwareverde/bitcoin/secp256k1/key/PrivateKey.h"
#include "com/softwareverde/bitcoin/secp256k1/key/PublicKey.h"
#include "com/softwareverde/bitcoin/wallet/SeedPhraseGenerator.h"
#include "com/softwareverde/util/BitcoinUtil.h"
#include "com/softwareverde/constable/bytearray/ImmutableByteArray.h"
#include "com/softwareverde/util/Secp256k1Util.h"
#include "com/softwareverde/util/Base64Util.h"
#include "com/softwareverde/util/Util.h"
#include "com/softwareverde/util/Container.h"
#include "com/softwareverde/bitcoin/secp256k1/signature/Signature.h"
#include "com/softwareverde/bitcoin/secp256k1/signature/SchnorrSignature.h"
#include "com/softwareverde/bitcoin/secp256k1/signature/Secp256k1Signature.h"
#include "com/softwareverde/util/HexUtil.h"
#include "com/softwareverde/security/pgp/PgpKeys.h"
#include "com/softwareverde/security/rsa/RsaKeys.h"
#include "com/softwareverde/security/aes/AesKey.h"
#include "com/softwareverde/api/ApiConfiguration.h"
#include "com/softwareverde/api/ApiCall.h"
#include "com/softwareverde/database/bitcoin/PlaceholderBitcoinDatabase.h"
#include "com/softwareverde/dublin/DublinSeedPhraseGenerator.h"
#include "com/softwareverde/dublin/identity/IdentityMetadata.h"
#include "com/softwareverde/dublin/identity/MetadataFieldType.h"
#include "com/softwareverde/dublinidentity/Identity.h"
#include "com/softwareverde/dublinidentity/util/CertificationUtil.h"
#include "com/softwareverde/dublinidentity/util/DerivedPrivateKeyCreator.h"
#include "com/softwareverde/dublinidentity/util/DerivedBitcoinPrivateKey.h"
#include "com/softwareverde/dublinidentity/api/J2ObjcApiCall.h"
#include "com/softwareverde/dublinidentity/api/IdentityJsonRequest.h"
#include "com/softwareverde/dublinidentity/api/BatchedIdentityJsonRequest.h"
#include "com/softwareverde/dublinidentity/api/CertificationResponse.h"
#include "com/softwareverde/dublinidentity/api/identity/SetKeyRingApiCall.h"
#include "com/softwareverde/dublinidentity/api/identity/SetKeyRingRequest.h"
#include "com/softwareverde/dublinidentity/api/identity/SetKeyRingResponse.h"
#include "com/softwareverde/dublinidentity/api/identity/GetKeyRingApiCall.h"
#include "com/softwareverde/dublinidentity/api/identity/GetKeyRingRequest.h"
#include "com/softwareverde/dublinidentity/api/identity/GetKeyRingResponse.h"
#include "com/softwareverde/dublinidentity/api/identity/GetSharedIdentitiesRequest.h"
#include "com/softwareverde/dublinidentity/api/identity/GetSharedIdentitiesResponse.h"
#include "com/softwareverde/dublinidentity/api/identity/GetSharedIdentitiesApiCall.h"
#include "com/softwareverde/dublinidentity/api/identity/GetSurveyIdentitiesRequest.h"
#include "com/softwareverde/dublinidentity/api/identity/GetSurveyIdentitiesResponse.h"
#include "com/softwareverde/dublinidentity/api/identity/GetSurveyIdentitiesApiCall.h"
#include "com/softwareverde/dublinidentity/api/identity/SetRelationshipDataRequest.h"
#include "com/softwareverde/dublinidentity/api/identity/SetRelationshipDataResponse.h"
#include "com/softwareverde/dublinidentity/api/identity/SetRelationshipDataApiCall.h"
#include "com/softwareverde/dublinidentity/api/certify/ServerPublicKeyRequest.h"
#include "com/softwareverde/dublinidentity/api/certify/ServerPublicKeyResponse.h"
#include "com/softwareverde/dublinidentity/api/certify/ServerPublicKeyApiCall.h"
#include "com/softwareverde/dublinidentity/api/certify/CreateSecondaryIdentityApiCall.h"
#include "com/softwareverde/dublinidentity/api/certify/CreateSecondaryIdentityRequest.h"
#include "com/softwareverde/dublinidentity/api/certify/CreateSecondaryIdentityResponse.h"
#include "com/softwareverde/dublinidentity/api/certify/ShareMetadataApiCall.h"
#include "com/softwareverde/dublinidentity/api/certify/ShareMetadataRequest.h"
#include "com/softwareverde/dublinidentity/api/certify/ShareMetadataResponse.h"
#include "com/softwareverde/dublinidentity/api/certify/CertifyUserDataRequest.h"
#include "com/softwareverde/dublinidentity/api/certify/CertifyUserDataApiCall.h"
#include "com/softwareverde/dublinidentity/api/certify/CertifyUserDataResponse.h"
#include "com/softwareverde/dublinidentity/api/certify/BatchedCertifyUserDataResponse.h"
#include "com/softwareverde/dublinidentity/api/certify/BatchedCertifyUserDataApiCall.h"
#include "com/softwareverde/dublinidentity/api/certify/RevokeMetadataRequest.h"
#include "com/softwareverde/dublinidentity/api/certify/RevokeMetadataResponse.h"
#include "com/softwareverde/dublinidentity/api/certify/RevokeMetadataApiCall.h"
#include "com/softwareverde/dublinidentity/api/certify/ShareDirection.h"
#include "com/softwareverde/dublinidentity/api/survey/GetPendingSurveyDataRequest.h"
#include "com/softwareverde/dublinidentity/api/survey/GetPendingSurveyDataResponse.h"
#include "com/softwareverde/dublinidentity/api/survey/GetPendingSurveyDataApiCall.h"
#include "com/softwareverde/dublinidentity/api/survey/RegisterSurveyRequest.h"
#include "com/softwareverde/dublinidentity/api/survey/RegisterSurveyResponse.h"
#include "com/softwareverde/dublinidentity/api/survey/RegisterSurveyApiCall.h"
#include "com/softwareverde/dublinidentity/api/survey/SubmitSurveyResponseRequest.h"
#include "com/softwareverde/dublinidentity/api/survey/SubmitSurveyResponseResponse.h"
#include "com/softwareverde/dublinidentity/api/survey/SubmitSurveyResponseApiCall.h"
#include "com/softwareverde/dublinidentity/api/survey/GetSurveyRequest.h"
#include "com/softwareverde/dublinidentity/api/survey/GetSurveyResponse.h"
#include "com/softwareverde/dublinidentity/api/survey/GetSurveyApiCall.h"
#include "com/softwareverde/dublinidentity/api/survey/SurveyQrCodeRequest.h"
#include "com/softwareverde/dublinidentity/api/survey/SurveyQrCodeResponse.h"
#include "com/softwareverde/dublinidentity/api/survey/SurveyQrCodeApiCall.h"
#include "com/softwareverde/dublinidentity/api/docchain/DocChainPassThroughRequest.h"
#include "com/softwareverde/dublinidentity/api/docchain/CreateDocumentRequest.h"
#include "com/softwareverde/dublinidentity/api/docchain/CreateDocumentResponse.h"
#include "com/softwareverde/dublinidentity/api/docchain/CreateDocumentApiCall.h"
#include "com/softwareverde/dublinidentity/api/docchain/FetchUserDocumentsRequest.h"
#include "com/softwareverde/dublinidentity/api/docchain/FetchUserDocumentsResponse.h"
#include "com/softwareverde/dublinidentity/api/docchain/FetchUserDocumentsApiCall.h"
#include "com/softwareverde/dublinidentity/api/docchain/FetchDocumentAndKeyRequest.h"
#include "com/softwareverde/dublinidentity/api/docchain/FetchDocumentAndKeyResponse.h"
#include "com/softwareverde/dublinidentity/api/docchain/FetchDocumentAndKeyApiCall.h"
#include "com/softwareverde/dublinidentity/request/UserDataRequest.h"
#include "com/softwareverde/docchain/DocChainConstants.h"
#include "com/softwareverde/docchain/client/UserDocumentWrapper.h"
#include "com/softwareverde/docchain/client/metadata/DocumentMetadata.h"
#include "com/softwareverde/docchain/client/metadata/MutableDocumentMetadata.h"
#include "com/softwareverde/bitcoin/hash/sha256/Sha256Hash.h"
#include "com/softwareverde/bitcoin/hash/sha256/ImmutableSha256Hash.h"
#include "com/softwareverde/bitcoin/hash/sha256/MutableSha256Hash.h"
#include "com/softwareverde/dublin/survey/Survey.h"
#include "com/softwareverde/dublin/survey/Question.h"
#include "com/softwareverde/dublinidentity/survey/SurveyData.h"
#include "com/softwareverde/dublinidentity/survey/UserSurveyResponse.h"
#include "com/softwareverde/dublinidentity/survey/SurveyResults.h"
#include "com/softwareverde/dublin/util/DocChainMessageUtil.h"
#include "com/softwareverde/dublin/util/ResultWrapper.h"

#include "com/softwareverde/logging/Logger.h"
#include "com/softwareverde/logging/LogFactory.h"
#include "com/softwareverde/logging/LogLevel.h"
#include "com/softwareverde/logging/StackTraceManager.h"
#include "com/softwareverde/logging/slf4j/Slf4jLogFactory.h"

#include "org/slf4j/impl/SimpleLogger.h"
#include "com/softwareverde/async/lock/IndexLock.h"

#endif