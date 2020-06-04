# OCN Node Changelog

## 1.1.0-rc1 
### Upcoming

Adds the ability for requests to be forwarded to "Ocn Apps" with matching permissions. The
Ocn App Interface, using the new Permissions contract in the OCN Registry, allows data to be
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