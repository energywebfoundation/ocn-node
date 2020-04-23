# OCN Node Changelog

## 1.1.0-rc0

Includes initial hubclientinfo OCPI module implementation
- Optional "Still-Alive" check requests connected parties versions endpoint at regular intervals.
- Optional "Planned Party" search scans registry for newly planned parties at regular intervals.
- New configuration properties under `ocn.node`: `stillAliveEnabled`, `stillAliveRate`, 
  `plannedPartySearchEnabled`, `plannedPartySearchRate`.
      
## 1.0.0 

Initial release of the Open Charging Network Node.
- All OCPI modules included, except for hubclientinfo.
- Custom OCPI module, *OcnRules*, for setting counter-party whitelist rules.
- Administrator API for generating TOKEN_A for planned parties.