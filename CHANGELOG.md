# OCN Node Changelog

## 1.1.1
### Nov 07, 2020

Fixes bug with deleted parties being communicated as having a "planned" status (ocn-node issue #22)

## 1.1.0
### Oct 16, 2020

1.1.0 release of Open Charging Network Node. Includes:

- Forwarding of requests to "Ocn Services" as specified by the OCN Service Interface
- Initial HubClientInfo module implementation

## 1.1.0-rc2
### Sep 25, 2020

Fixes bug with handling request body when forwarding custom OCPI module requests.

## 1.1.0-rc1 
### Jun 30, 2020

Adds the ability for requests to be forwarded to "Ocn Services" with matching permissions. The
Ocn ServiceInterface, using the new Permissions contract in the OCN Registry, allows data to be
shared and accessed using a permission system.

## 1.1.0-rc0 
### Apr 28, 2020

Includes initial hubclientinfo OCPI module implementation.
- Optional "Still-Alive" check requests connected parties versions endpoint at regular intervals.
- Optional "Planned Party" search scans registry for newly planned parties at regular intervals.
- New configuration properties under `ocn.node`: `stillAliveEnabled`, `stillAliveRate`, 
  `plannedPartySearchEnabled`, `plannedPartySearchRate`.
      
## 1.0.0
### Mar 03, 2020

Initial release of the Open Charging Network Node.
- All OCPI modules included, except for hubclientinfo.
- Custom OCPI module, *OcnRules*, for setting counter-party whitelist rules.
- Administrator API for generating TOKEN_A for planned parties.