package org.tupol.demo.streaming

/**
 *
 */
package object states {

  trait StateUpdater[StateType, RecordType] {
    def update(record: RecordType): StateType
    def update(records: Iterable[RecordType]): StateType
    /** Alias for `update` */
    def |+|(record: RecordType) = update(record)
    /** Alias for `update` */
    def |+|(records: Iterable[RecordType]) = update(records)
  }

}
