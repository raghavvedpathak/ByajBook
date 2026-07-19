package com.byajbook.domain.exception

/**
 * [FIX-LINKEDEXCEPTION-1] RecordLinkedTakenException definition — define in :core:domain
 * so both the repository and ViewModel can reference it.
 */
class RecordLinkedTakenException(val linkedCount: Int) : 
    Exception("Cannot delete GIVEN record: $linkedCount TAKEN record(s) are linked to it")
